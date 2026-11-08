package ac.warwick.wfc;

import ac.warwick.wfc.Building;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.json.simple.JSONObject;

import ac.warwick.core.Bitmap;
import ac.warwick.core.Renderer;
import netscape.javascript.JSException;
import java.io.FileReader;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class WFCViewer {
    private WFC wfc;
    private Bitmap bitmap;

    private int output_width;
    private int output_height;

    //Earthquake damage colours
    private static final int DAMAGE_LEVEL_1 = 0xFF22A06B; // Green
    private static final int DAMAGE_LEVEL_2 = 0xFFD6B72C; // Yellow
    private static final int DAMAGE_LEVEL_3 = 0xFFE58A32; // Orange
    private static final int DAMAGE_LEVEL_4 = 0xFFD94A45; // Red
    private static final int DAMAGE_LEVEL_5 = 0xFF8D2525; // Dark red

    public WFCViewer(Renderer renderer) {
        BufferedImage image = null;
        try {
            image = ImageIO.read(new File("res/city-3.png"));
        } catch (IOException e) {
            System.out.println("Failed to load image");
        }

        int width = image.getWidth();
        int height = image.getHeight();
        this.wfc = new WFC(
            image.getRGB(0, 0, width, height, null, 0, width),
            width,
            height,
            3
        );

        this.output_width = this.wfc.get_image_width();
        this.output_height = this.wfc.get_image_height();

        this.bitmap = new Bitmap(
            renderer, this.output_width, this.output_height);

        this.run_wfc();
        this.apply_earthquake_damage();
        renderer.enqueue_resource(this.bitmap);
    }

    public void run_wfc() {
        this.wfc.collapse();
        this.bitmap.from_int_array(this.wfc.get_pixels());
        this.bitmap.send_to_gpu();
    }

    // gets a single pixel from our output image
    public int get_pixel(int x, int y) {
        return this.wfc.get_pixels()[x + y * this.output_width];
    }

    public boolean blank(int col) {
        return (col == 0xff000000 || col == 0x00);
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
            start_x,
            this.output_height - height,
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
    public void serialize() {
        Building[] buildings = this.find_buildings();
        JSONObject output = new JSONObject();

        for (int i = 0; i < buildings.length; i++) {
            output.put(i, buildings[i].serialize());
        }

        System.out.println(output);
        this.find_buildings();

        // finally write our buildings to a file
        try (FileWriter file = new FileWriter("buildings.json")) {
            output.writeJSONString(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int get_damage_colour(int level) {
        switch (level) {
            case 1: return DAMAGE_LEVEL_1;
            case 2: return DAMAGE_LEVEL_2;
            case 3: return DAMAGE_LEVEL_3;
            case 4: return DAMAGE_LEVEL_4;
            case 5: return DAMAGE_LEVEL_5;
            default: return 0xFFFFFFFF;
        }
    }

    public void apply_earthquake_damage() {

        JSONParser parser = new JSONParser();

        try (FileReader reader = new FileReader("earthquake_results.json")) {

            JSONObject results = (JSONObject) parser.parse(reader);
            JSONObject building_results = (JSONObject) results.get("buildings");

            Building[] buildings = this.find_buildings();

            for (int i = 0; i < buildings.length; i++) {

                Building building = buildings[i];

                JSONObject result = (JSONObject) building_results.get(
                    String.valueOf(i)
                );

                if (result == null) {
                    System.out.println("Missing earthquake result for building " + i);
                    continue;
                }

                int damage_level = ((Number) result.get("damage_level")).intValue();
                int damage_colour = get_damage_colour(damage_level);

                // Recolour every pixel belonging to this building.
                for (int x = building.x; x < building.x + building.width; x++) {

                    for (int y = building.y; y < building.y + building.height; y++) {

                        this.bitmap.set_pixel(x, y, damage_colour);
                    }
                }
            }

            this.bitmap.send_to_gpu();

            System.out.println("Earthquake damage visualisation applied.");

        } catch (IOException | ParseException e) {
            System.out.println("Failed to load earthquake results.");
            e.printStackTrace();
        }
    }
}