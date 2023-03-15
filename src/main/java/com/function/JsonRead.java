package com.function;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;



import org.json.simple.parser.*;


import java.net.http.HttpRequest;

import okhttp3.MediaType;
//import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
/**
 * Azure Functions with HTTP Trigger.
 */
public class JsonRead {

    @FunctionName("JsonRead")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET, HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) throws ParseException {
        context.getLogger().info("Java HTTP trigger processed a request.");
        
        String requestBody =  request.getBody().orElse(null);

        //String requestBody= "{\"entry\":[{\"Business component\":\"h_dashboard_v2\",\"BG\":\" UXP\",\"Short description keyID\":\"\",\"FR\":\" UXP\",\"Key\":\"dashboard.fbstatus.xp.labelxp\"},{\"Business component\":\"h_dashboard_v2\",\"BG\":\"???????? ??????T\",\"Short description keyID\":\"\",\"FR\":\"???????? ??????? \u2013\",\"Key\":\"dashboard.lasttransaction.label\"}]}";

        Object obj = new JSONParser().parse(requestBody);
        JSONObject json = (JSONObject) obj;
        System.out.println("all:"+json);

       JSONObject jsonData = (JSONObject) json.get("params");
       System.out.println(jsonData);
       JSONArray array = (JSONArray) jsonData.get("entry");


        JSONObject jsonObj = (JSONObject) array.get(0);
        Set<String> keys = jsonObj.keySet();

        for (String title : keys) {
            
            if (!(title.equals("Short description keyID")  
                || title.equals("Business component") 
                || title.equals("Key")) ){

                String langue = title;
                System.out.println(langue);

                JSONObject content = new JSONObject();

                for (Object elementArray : array) {

                    JSONObject elementJson = (JSONObject) elementArray;
                    String key = (String) elementJson.get("Key");
                    String value = (String) elementJson.get(langue);

                    content.put(key, value);

                }

                System.out.println(content.toString());

                OkHttpClient client = new OkHttpClient();

                MediaType mediaType = MediaType.parse("application/json");
           
                RequestBody body = RequestBody.create(content.toJSONString(), mediaType);
                Request requestCreateOneEntry = new Request.Builder()
                    .url("https://eu-api.contentstack.com/v3/content_types/categories/entries?locale=fr")
                    .post(body)
                    .addHeader("api_key", "blt0e7212638c9ff7cd")
                    .addHeader("authorization", "csa27268198a98c8d71ea5445e")
                    .addHeader("Content-Type", "application/json")
                    .build();
            try (Response responseCreateOneEntry = client.newCall(requestCreateOneEntry).execute()) {
                //System.out.println("response a analyser"+response.toString());
                if (responseCreateOneEntry.code()==200) {
                    String responseBody = responseCreateOneEntry.body().string();
                    System.out.println("La requete de creation d'une entry a bien marche et retourne :\n"+responseBody);
                } else {
                    System.out.println("PB requete de creation d'une entry, le Code retour="+responseCreateOneEntry.code());
                }
            } catch (Exception e) {
                System.out.println("pb avec l'execution de la requete de creation d'une entry");
            }


                

            }

                
        }

        if (requestBody == null || requestBody.isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("pass a valid JSON ")
                    .build();
        } else {

                        
            return request.createResponseBuilder(HttpStatus.OK)
                    .header("Content-Type", "application/json")
                    .body("requestBody : coucou")
                    .build();


        }

  


 




    }


}
