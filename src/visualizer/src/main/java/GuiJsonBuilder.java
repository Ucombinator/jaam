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

public class GuiJsonBuilder
{
    public static class VertexSerializer implements JsonSerializer<Vertex>
    {
        public JsonElement serialize(final Vertex vertex, final Type type, final JsonSerializationContext context)
        {
            System.out.println("Serializing vertex: " + Integer.toString(vertex.id));
            JsonObject jsonVertex = new JsonObject();
            jsonVertex.add("id", new JsonPrimitive(vertex.id));
            jsonVertex.add("label", new JsonPrimitive(vertex.getDescription()));
            return jsonVertex;
        }
    }

    public static class EdgeSerializer implements JsonSerializer<Edge>
    {
        public JsonElement serialize(final Edge edge, final Type type, final JsonSerializationContext context)
        {
            System.out.println("Serializing edge");
            JsonObject jsonEdge = new JsonObject();
            jsonEdge.add("id", new JsonPrimitive(0)); // TODO: Assign ID's to edges
            jsonEdge.add("from", new JsonPrimitive(edge.source));
            jsonEdge.add("to", new JsonPrimitive(edge.dest));
            jsonEdge.add("label", new JsonPrimitive(""));
            return jsonEdge;
        }
    }

    public static class GraphSerializer implements JsonSerializer<Graph>
    {
        public JsonElement serialize(final Graph graph, final Type type, final JsonSerializationContext context)
        {
            System.out.println("Serializing graph");
            Gson gson = new GsonBuilder().setPrettyPrinting()
                    .registerTypeAdapter(Vertex.class, new VertexSerializer())
                    .registerTypeAdapter(Edge.class, new EdgeSerializer()).create();
            JsonObject jsonGraph = new JsonObject();
            jsonGraph.addProperty("nodes", gson.toJson(graph.vertices));
            jsonGraph.addProperty("edges", gson.toJson(graph.baseEdges));
            return jsonGraph;
        }
    }

    public static void printGuiJson()
    {
        Gson gson = new GsonBuilder().setPrettyPrinting()
                .registerTypeAdapter(Graph.class, new GraphSerializer()).create();
        System.out.println(gson.toJson(Main.graph));
    }
}
