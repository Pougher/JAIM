
package ac.warwick.wfc;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import org.json.simple.JSONObject;

public class WFCSerializer {
    private int[] pixels;
    private int output_width;
    private int output_height;

    // Copies information from the WFC into the serializer.
    public WFCSerializer(int[] pix, int ow, int oh) {
        this.pixels = pix;
        this.output_width = ow;
        this.output_height = oh;
    }

    // Gets a single pixel from our output image.
    public int get_pixel(int x, int y) {
        return this.pixels[x + y * this.output_width];
    }

    public boolean blank(int col) {
        return col == 0xff000000 || col == 0x00 || col == 0xffffffff;
    }

    // Detects a rectangular building starting at start_x.
    public Building find_building_group(int start_x) {
        int height = 0;
        int width = 0;

        ArrayList<Integer> materials = new ArrayList<>();

        // Extend upwards from the bottom of the city.
        while (height < this.output_height
            && !this.blank(
                this.get_pixel(start_x, this.output_height - height - 1))) {
            height++;
        }

        // Extend horizontally while the columns belong to this building.
        for (int x = start_x; x < this.output_width; x++) {
            boolean valid_column = true;

            // Check whether the entire column belongs to the building.
            for (int y = this.output_height - 1;
                 y >= this.output_height - height;
                 y--) {

                if (this.blank(this.get_pixel(x, y))) {
                    valid_column = false;
                    break;
                }
            }

            // Check whether this column extends above the building.
            if (valid_column && height < this.output_height
                && !this.blank(
                    this.get_pixel(x, this.output_height - height - 1))) {
                valid_column = false;
            }

            if (!valid_column) {
                break;
            }

            // Store the materials in this column.
            for (int y = this.output_height - 1;
                 y >= this.output_height - height;
                 y--) {

                materials.add(this.get_pixel(x, y));
            }

            width++;
        }

        int building_y = this.output_height - height;

        return new Building(
            start_x,
            building_y,
            width,
            height,
            materials.stream()
                .mapToInt(Integer::intValue)
                .toArray()
        );
    }

    public Building[] find_buildings() {
        ArrayList<Building> buildings = new ArrayList<>();

        int x_pos = 0;

        while (x_pos < this.output_width) {

            if (this.blank(
                this.get_pixel(x_pos, this.output_height - 1))) {
                x_pos++;
                continue;
            }

            Building building = this.find_building_group(x_pos);

            buildings.add(building);

            x_pos += Math.max(1, building.width);
        }

        return buildings.toArray(new Building[0]);
    }

    // Converts building data into JSON.
    @SuppressWarnings("unchecked")
    public void serialize(String path) {
        Building[] buildings = this.find_buildings();

        JSONObject output = new JSONObject();

        for (int i = 0; i < buildings.length; i++) {
            output.put(
                String.valueOf(i),
                buildings[i].serialize()
            );
        }

        // Write building data to a file.
        try (FileWriter file = new FileWriter(path)) {
            output.writeJSONString(file);

            System.out.println(
                "Successfully exported " + buildings.length
                + " buildings to " + path
            );

        } catch (IOException e) {
            System.err.println("Failed to export buildings:");
            e.printStackTrace();
        }
    }
}
