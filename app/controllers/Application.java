package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import play.Logger;
import play.libs.Json;
import play.mvc.*;
import play.libs.ws.*;

import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;


public class Application extends Controller {

  @Inject
  WSClient ws;

  @Inject
  Logger logger;

  public static class LatLng {
    public double lat;
    public double lng;
  }

  public static class RequestInfo {
    public Integer radius;
    public LatLng[] locations;
  }

  public static class RouteNode {
    public Integer id;
    public Integer typeId;
    public String name;
    public JsonNode coordinates;
  }

  public static class RatingNode{
    public Integer counter;
    public RouteNode route;
  }

  public Result index()  {
    return ok(views.html.main.render());
  }

  public CompletionStage<WSResponse> callWikiService(Integer radius, Double lat, Double lng ){
    WSRequest request = ws.url("http://wikiroutes.info/userService/radiusSearchTemporary");
    request.setContentType("application/json; charset=UTF-8");
    request.setQueryParameter("radius",radius.toString());
    request.setQueryParameter("lat",lat.toString());
    request.setQueryParameter("lon",lng.toString());
    Logger.info("Init request {} {} on thread {}", lat.toString(), lng.toString(),Thread.currentThread().getId());
    CompletionStage<WSResponse> responsePromise = request.get();
    return responsePromise;
  }

  public JsonNode getJson( WSResponse response){
    JsonNode result = null;
    try {
      result = response.asJson();
    }catch(Throwable e){
      logger.info("getJson Error {}", e.getMessage()  );
    }
    return result;
  }



  public void saveResponse(HashMap<Integer, RatingNode> rating, WSResponse response){
    Logger.info("Save response on thread {}", Thread.currentThread().getId());
    JsonNode jsn = getJson(response);
    if (jsn == null){
      return;
    }

    for (Iterator<JsonNode> it = jsn.elements(); it.hasNext(); ) {
      JsonNode rjsn = it.next();
      try {
        RouteNode rNode = Json.fromJson(rjsn, RouteNode.class);
        RatingNode ratNode = rating.get(rNode.id);
        if (ratNode != null){
          ratNode.counter++;
        }else {
          ratNode = new RatingNode();
          ratNode.counter = 1;
          ratNode.route = rNode;
        }
        rating.put(rNode.id, ratNode);

      }catch (Throwable e){
        logger.info("saveResponse Error {}", e.getMessage()  );
      }

    }
  }


  public Result makeResult(HashMap<Integer, RatingNode> rating) {
    List<RatingNode> list = rating.values().stream()
            .sorted((r2,r1) -> Integer.compare(r1.counter,r2.counter))
            .limit(10)
            .collect(Collectors.toList());
    return ok(Json.toJson(list));
  }


  public CompletionStage<Result> calculate() {
    HashMap<Integer, RatingNode> rating = new HashMap<>();
    CompletionStage<Result> result = null;
    ArrayList<CompletableFuture> arrayList = new ArrayList<>();
    JsonNode json = request().body().asJson();
    if (json == null) {
      return CompletableFuture.completedFuture(0).thenApply((response)->badRequest("Expecting Json data"));
    };
    try {
      RequestInfo ri = Json.fromJson(json, RequestInfo.class);
      for(Integer i = 0; i < ri.locations.length; i++) {
        arrayList.add(
                callWikiService(ri.radius, ri.locations[i].lat, ri.locations[i].lng)
                        .thenAccept(response -> this.saveResponse(rating, response))
                        .toCompletableFuture());
      }
      CompletableFuture[] a = arrayList.toArray(new CompletableFuture[arrayList.size()]);
      result = CompletableFuture.allOf(a).thenApply(ignoredVoid -> this.makeResult(rating));
    }

    catch(Throwable e) {
      Logger.error(e.getMessage());
    }
    return result;
  }
}
