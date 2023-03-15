package com.function;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

import java.util.Iterator;
import java.util.Optional;
import java.util.Set;


import org.json.simple.JSONArray;
import org.json.simple.JSONObject;



import org.json.simple.parser.*;
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

        //

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
                            System.out.println(elementJson.get("Key"));


                        }

                        // for (int i = 0; i < array.length(); i++)
                        // {
                        // String key = array.getJSONObject(i).getString("Key");
                        // String value=array.getJSONObject(i).getString(langue);

                        // System.out.println("key:" +key +" "+ "value:" +value);
                     
                        // }

                }
                    
            }

       // }


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
