package com.function;

import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.functions.*;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

        
        // List<> listJson = new ArrayList<String>();
        
        String requestBody =  request.getBody().orElse(null);

        //Array t = requestBody.a

        // List<> list = requestBody.toArray();

        // Create String[] from setOfString
      
 
        // return the formed String[]

        // String arrayJson = "[[\"test\",\"test2\"],[\"test3\",\"test3\"]]";

        // JSON.parse(arrayJson);
    
       // String str = "[{\"name\":\"name1\",\"url\":\"url1\"},{\"name\":\"name2\",\"url\":\"url2\"}]";

        Object obj = new JSONParser().parse(requestBody);
        JSONObject json = (JSONObject) obj;
        System.out.println("all:"+json);

       

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

        // 

        



       





    
    }

    /*public static void main(String[] args) {

        String requestBody = "{\"test1\":{\"Business component\": \"h_dashboard_v2\",\"Key\": \"dashboard.fbstatus.xp.labelxp\",\"BG\": \"{1} UXP\",\"DA\": \"{1} UXP\"},\"test2\":{\"Business component\": \"h_dashboard_v2\",\"Key\": \"dashboard.lasttransaction.label\",\"BG\": \"Последна дейност {1} {2}\",\"DA\": \"Sidste aktivitet {1} {2}\" }}";
        //String requestBody = "{\"test\":\"test\"}";
        
        try {
            Object obj = new JSONParser().parse(requestBody);
            JSONObject json = (JSONObject) obj;
            System.out.println("all: "+json);
        } catch (Exception e) {
            
            System.out.println(e);
        }
    }*/
}
