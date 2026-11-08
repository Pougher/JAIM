
package ac.warwick.wfc;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Building {
    public int x;
    public int y;
    public int width;
    public int height;
    public int[] materials;

    public Building(int x, int y, int width, int height, int[] data) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.materials = data;
    }

    public void print() {
        System.out.println(
            "BUILDING :: Position: (" + x + ", " + y + ") "
            + "Width: " + width + " Height: " + height
        );
    }

    @SuppressWarnings("unchecked")
    public JSONObject serialize() {
        JSONObject obj = new JSONObject();
        JSONArray mats = new JSONArray();

        for (int material : this.materials) {
            mats.add(Integer.toUnsignedLong(material));
        }

        obj.put("x", this.x);
        obj.put("y", this.y);
        obj.put("width", this.width);
        obj.put("height", this.height);
        obj.put("materials", mats);

        return obj;
    }
}
