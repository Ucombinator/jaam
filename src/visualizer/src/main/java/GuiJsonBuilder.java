/**
 * Created by timothyjohnson on 10/31/16.
 *
 * This is used to serialize/deserialize between Java and Javascript.
 */

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonSerializationContext;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.lang.reflect.Type;

import java.lang.reflect.Type;

public class GuiJsonBuilder
{
    public static class GraphSerializer implements JsonSerializer<Graph> {
        public JsonElement serialize(final Graph graph, final Type type, final JsonSerializationContext context) {
            JsonObject result = new JsonObject();
            result.add("vertices", new JsonPrimitive(graph.totalVertices));
            return result;
        }
    }

    public static void printGuiJson()
    {
        Gson gson = new GsonBuilder().setPrettyPrinting()
                .registerTypeAdapter(Graph.class, new GraphSerializer()).create();
        System.out.println(gson.toJson(Main.graph));
    }
}
