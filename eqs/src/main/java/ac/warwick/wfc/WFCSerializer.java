package ac.warwick.wfc;

import ac.warwick.wfc.Building;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import java.util.Objects;
import org.json.simple.JSONObject;

import ac.warwick.core.Bitmap;
import ac.warwick.core.Renderer;
import netscape.javascript.JSException;

public class WFCSerializer {
    private int[] pixels;
    private int output_width;
    private int output_height;

    // copies some information from the WFC into the serializer. Calling
    // serialize() saves the JSON data
    public WFCSerializer(int[] pix, int ow, int oh) {
        this.pixels = pix;
        this.output_width = ow;
        this.output_height = oh;
    }

    // gets a single pixel from our output image
    public int get_pixel(int x, int y) {
        return this.pixels[x + y * this.output_width];
    }

    public boolean blank(int col) {
        return (col == 0xff000000 || col == 0x00 || col == 0xffffffff);
    }

    // starting from the ground, we will attempt to extend our building to the
    // highest Y value possible. Then we will extend it across until we
    // encounter a different X pixel. A black pixel is NOT a building
    public Building find_building_group(int start_x) {
        int height = 0;
        int width = 0;
        ArrayList<Integer> materials = new ArrayList<>();

        // first extrude up
        while (height < this.output_height
            && !this.blank(
                this.get_pixel(start_x, this.output_height - height - 1))) {
            height++;
        }

        for (int x = start_x; x < this.output_width; x++) {
            boolean valid_column = true;

            // Check whether the entire column belongs to the building
            for (int y = this.output_height - 1;
                y >= this.output_height - height;
                y--) {

                if (this.blank(this.get_pixel(x, y))) {
                    valid_column = false;
                    break;
                }
            }
            
            // if we aren't at the top, a taller building could be next to this
            // one, so check the pixel above
            if (height < this.output_height) {
                if (!this.blank(
                    this.get_pixel(x, this.output_height - height - 1))) {
                    // we are in another building
                    valid_column = false;
                }
            }

            // Stop if we encounter a black pixel
            if (!valid_column) break;

            // Store the materials in this column
            for (int y = this.output_height - 1;
                 y >= this.output_height - height;
                y--) {

                materials.add(this.get_pixel(x, y));
            }

            width++;
        }

        return new Building(
            width,
            height,
            materials
                .stream()
                .mapToInt(Integer::intValue)
                .toArray());
    }

    public Building[] find_buildings() {
        ArrayList<Building> buildings = new ArrayList<>();
        int x_pos = 0;

        while (x_pos < this.output_width) {
            if (this.blank(this.get_pixel(x_pos, this.output_height - 1))) {
                x_pos++;
                continue;
            }

            // if the pixel isn't black we've found a building!
            Building b = this.find_building_group(x_pos);
            buildings.add(b);

            x_pos += b.width;
        }

        return buildings.toArray(new Building[0]);
    }

    // converts all of the data we have generated into JSON
    public void serialize(String path) {
        Building[] buildings = this.find_buildings();
        JSONObject output = new JSONObject();

        for (int i = 0; i < buildings.length; i++) {
            output.put(i, buildings[i].serialize());
        }

        System.out.println(output);
        this.find_buildings();

        // finally write our buildings to a file
        try (FileWriter file = new FileWriter(path)) {
            output.writeJSONString(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
