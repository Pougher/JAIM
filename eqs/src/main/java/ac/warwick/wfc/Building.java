package ac.warwick.wfc;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

// class which acts as a data storage structure for the buildings which are
// going to be generated. Materials is basically just the building's data packed
// as an array
public class Building {
    public int width;
    public int height;
    public int[] materials;

    public Building(int width, int height, int[] data) {
        this.width = width;
        this.height = height;
        this.materials = data;
    }

    public void print() {
        System.out.println(
            "BUILDING :: Width: " + this.width + "Height: " + this.height);
    }

    public JSONObject serialize() {
        JSONObject obj = new JSONObject();
        JSONArray mats = new JSONArray();

        for (int material : this.materials) {
            mats.add(Integer.toUnsignedLong(material));
        }

        obj.put("width", this.width);
        obj.put("height", this.height);
        obj.put("materials", mats);
        
        return obj;
    }
}