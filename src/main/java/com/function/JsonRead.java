package com.function;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class JsonRead {

    @FunctionName("JsonRead")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = { HttpMethod.GET,
            HttpMethod.POST }, authLevel = AuthorizationLevel.ANONYMOUS) HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) throws ParseException, InterruptedException, IllegalAccessException, IOException {
        context.getLogger().info("Java HTTP trigger processed a request.");
        String output = "";

        //recupération du body envoyé et transformation en JSON
        //{"headers":{"Content-Type":"application\/json"},"params":{"entry":[{"DE":"test labelxp","Business component":"test_import","Short description keyID":"","FR":"test labelxp","Key":"labelxp"},{"DE":"test label","Business component":"test_import","Short description keyID":"","FR":"test label","Key":"label"}]}}
        String requestBody = request.getBody().orElse(null);
        Object obj = new JSONParser().parse(requestBody);
        JSONObject json = (JSONObject) obj;

        //Descent jusqu'à entry [{"DE":"test labelxp","Business component":"test_import","Short description keyID":"","FR":"test labelxp","Key":"labelxp"},{"DE":"test label","Business component":"test_import","Short description keyID":"","FR":"test label","Key":"label"}]
        JSONObject jsonData = (JSONObject) json.get("params");
        JSONArray array = (JSONArray) jsonData.get("entry");

        //Recupération du content type name
        JSONObject jsonObj = (JSONObject) array.get(0);
        String contentTypeName = (String) jsonObj.get("Business component");

        //Récupération des clefs 
        //{"DE","Business component","Short description keyID","FR","Key"}
        //Set<String> keys = jsonObj.keySet();
        // keys.remove("Business component");
        // keys.remove("Short description keyID");
        // keys.remove("Key");
        //{"DE","FR"}

        Set<String> keysToRemove = new HashSet<>(Arrays.asList("Business component", "Short description keyID", "Key"));
        Set<String> keys = jsonObj.keySet();
        keys.removeAll(keysToRemove);


        //Récuparation de la liste des Fields
        List<String> listFieldsContentType = new ArrayList<String>();
        for (Object elementArray : array) {
            JSONObject elementJson = (JSONObject) elementArray;
            String key = (String) elementJson.get("Key");
            listFieldsContentType.add(key);
        }

        //Chercher si le content Type existe déjà ou pas
        String resSearchContenType = SearchContentType(contentTypeName);
        System.out.println("resultat search: "+resSearchContenType);

        String stringBodyJson=genrateContentTypeJSONFromListOfFields(contentTypeName,listFieldsContentType); 
        //Si reponse == 200 c'est qu'il a trouvé un content Type
        if (resSearchContenType.equals("200")){
            String resUpdateContentType = UpdateContentType(contentTypeName,stringBodyJson);
            output = output.concat(resUpdateContentType);            
        }else{
            //Creation du Content Type
            String resCreateContentType = CreateContentType(stringBodyJson);
            output = output.concat(resCreateContentType);            
        }

        for (String langue : keys) { //{"DE","FR"}
           if (!(langue.equals("Short description keyID") || langue.equals("Business component") || langue.equals("Key"))) {

                JSONObject content = new JSONObject();
                content.put("title", "import_" + langue);

                for (Object elementArray : array) {
                    JSONObject elementJson = (JSONObject) elementArray;
                    String key = (String) elementJson.get("Key");
                    String value = (String) elementJson.get(langue);
                    content.put(key, value);
                }

                JSONObject contentAll = new JSONObject();
                contentAll.put("entry", content);

                //Regarde si l'entry existe
                String uid = Search("import_" + langue, contentTypeName);

                //si l'entry existe on update, sinon on créer
                if (uid.equals("")) {
                    String res = CreateEntry(contentTypeName, uid, langue.toLowerCase(), contentAll);
                    output = output.concat(res);
                } else {
                    String res = UpdateEntry(contentTypeName, uid, langue.toLowerCase(), contentAll);
                    output = output.concat(res);
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
                    .body("requestBody : " + output)
                    .build();
        }

    }

    public static String Search(String name, String contentTypeName) {
        String output = " ";

        OkHttpClient client = new OkHttpClient();

        //Build de la requête
        Request requestSearchEntry = new Request.Builder()
                .url("https://eu-api.contentstack.com/v3/content_types/" + contentTypeName+ "/entries?query={\"title\":\"" + name +"\"}")
                .get()
                .addHeader("api_key", "blt0e7212638c9ff7cd")
                .addHeader("access_token", "csa27268198a98c8d71ea5445e")
                .addHeader("Content-Type", "application/json")
                .addHeader("authtoken", "blt57a01bcc35b524ed")
                .build();
        
        //Execution de la requête
        try (Response responseSearchEntry = client.newCall(requestSearchEntry).execute()) {
            if (responseSearchEntry.code() == 200) {
                String responseBody = responseSearchEntry.body().string();
                Object obj = new JSONParser().parse(responseBody);
                JSONObject json = (JSONObject) obj;
                JSONArray array = (JSONArray) json.get("entries");
                //Si l'array est vide c'est qu'il n'a rien trouvé donc l'entry n'existe pas, sinon on return l'uid
                if (array.size() == 0) {
                    output = "";
                } else {
                    JSONObject jsonObj = (JSONObject) array.get(0);
                    String uid = (String) jsonObj.get("uid");
                    output = uid;
                }
            } else {
                output = output.concat("<br/> PB Search Entry: " + responseSearchEntry.code());
                System.out.println(output);
            }
        } catch (Exception e) {
            output = output.concat("<br/> PB Search Entry : " + e);
            System.out.println(output);
        }
        return output;
    }

    public static String UpdateEntry(String contentTypeName, String uid, String langue, JSONObject contentAll) {
        String output = " ";

        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(contentAll.toJSONString(), mediaType);

        //Build de la requête
        Request requestUpdateOneEntry = new Request.Builder()
                .url("https://eu-api.contentstack.com/v3/content_types/" + contentTypeName + "/entries/" + uid+ "?locale=" + langue)
                .put(body)
                .addHeader("api_key", "blt0e7212638c9ff7cd")
                .addHeader("authorization", "csa27268198a98c8d71ea5445e")
                .addHeader("Content-Type", "application/json")
                .build();
        

        //Execution de la requête
        try (Response responseUpdateOneEntry = client.newCall(requestUpdateOneEntry).execute()) {
            if (responseUpdateOneEntry.code() == 200) {
                String responseBody = responseUpdateOneEntry.body().string();
                output = output.concat("<br/> Succesfuly Updated " + uid);
            } else {
                output = output.concat("<br/> PB Update: " + responseUpdateOneEntry.code());
            }
        } catch (Exception e) {
            output = output.concat("<br/> PB Update: " + e);
        }
        return output;
    }

    public static String CreateEntry(String contentTypeName, String uid, String langue, JSONObject contentAll) {
        String output = "";

        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(contentAll.toJSONString(), mediaType);

        //Build de la requête
        Request requestCreateOneEntry = new Request.Builder()
                .url("https://eu-api.contentstack.com/v3/content_types/" + contentTypeName + "/entries?locale="+ langue)
                .post(body)
                .addHeader("api_key", "blt0e7212638c9ff7cd")
                .addHeader("authorization", "csa27268198a98c8d71ea5445e")
                .addHeader("Content-Type", "application/json")
                .build();

        //Execution de la requête                
        try (Response responseCreateOneEntry = client.newCall(requestCreateOneEntry).execute()) {
            if (responseCreateOneEntry.code() == 201) {
                String responseBody = responseCreateOneEntry.body().string();
                Object objectReponsObject = new JSONParser().parse(responseBody);
                JSONObject jsonReponse = (JSONObject) objectReponsObject;
                JSONObject entryData = (JSONObject) jsonReponse.get("entry");
                String createdUid = (String) entryData.get("uid");
                output = output.concat("<br/> Succefully Created " + createdUid);
            } else {
                output = output.concat("<br/> PB requete de creation d'une entry, le Code retour: " + responseCreateOneEntry.code());
            }

        } catch (Exception e) {
            output = output.concat("<br/> PB avec l'execution de la requete de creation d'une entry");
        }
        return output;
    }

    private static String cleanFieldName(String fieldName) {
        String result=fieldName.toLowerCase();
        result=result.replace("-","_");
        result=result.replace(".","_");
        result=result.replace(" ","_");
        return result;
     }

    public static String genrateContentTypeJSONFromListOfFields(String nameOfContentType, List<String> listFieldsContentType) {
        String stringBodyJson="";
        stringBodyJson="{\"content_type\":{\"title\": \""+nameOfContentType+"\",\"uid\": \""+cleanFieldName(nameOfContentType)+"\",\"schema\": [";
        // Title is mandatory and alway like that
        stringBodyJson+="{\"display_name\": \"Title\",\"uid\": \"title\",\"data_type\": \"text\",\"mandatory\": true,\"unique\": true,\"field_metadata\": {\"_default\": true},\"multiple\": false},";
        for (int i = 0; i < listFieldsContentType.size(); i++) {
           String fieldName=listFieldsContentType.get(i);
           stringBodyJson+="{\"data_type\": \"text\",\"display_name\": \""+fieldName+"\",\"uid\": \""+cleanFieldName(fieldName)+"\",\"field_metadata\": {\"description\": \"\",\"default_value\": \"\"},";
           stringBodyJson+="\"format\": \"\",\"error_messages\": {\"format\": \"\"},\"multiple\": false,\"mandatory\": false,\"unique\": false}";
           if (i<listFieldsContentType.size()-1) {
              stringBodyJson+=",";
           }
        }
        stringBodyJson+="]}}";
        return stringBodyJson;
     }

    public static String CreateContentType(String stringContentTypeJson) throws IOException {
        String output = "";

        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(stringContentTypeJson,mediaType);

        //Build de la requête
        Request createContentType = new Request.Builder()
           .url("https://eu-api.contentstack.com/v3/content_types/")
           //.method("POST", body)  Seems to be deprecated
           .post(body)
           .addHeader("api_key", "blt0e7212638c9ff7cd")
           .addHeader("authorization", "csa27268198a98c8d71ea5445e")
           .addHeader("Content-Type", "application/json")
           .build();

        //Execution de la requête
        try (Response responseCreateOneContentType = client.newCall(createContentType).execute()) {
           //System.out.println("response a analyser"+responseCreateOneContentType.toString());
           if (responseCreateOneContentType.code()<300) {
              output=output.concat("<br/> La requete de creation d'une Content Type a bien marche et retourne :\n"+responseCreateOneContentType.body().string());

           } else {
              output=output.concat("<br/> PB requete de creation d'une Content Type, le Code retour="+responseCreateOneContentType.code()+"\nPeut-etre le content type existe deja!");
           }
        } catch (Exception e) {
           output=output.concat("<br/> pb avec l'execution de la requete de creation d'un content type");
        }
        return output;
    }

    public static String SearchContentType(String contentTypeName) {
        String output = " ";

        //Build de la requête
        OkHttpClient client = new OkHttpClient();
        Request requestSearchContentType= new Request.Builder()
                .url("https://eu-api.contentstack.com/v3/content_types/"+contentTypeName)
                .get()
                .addHeader("api_key", "blt0e7212638c9ff7cd")
                .addHeader("access_token", "csa27268198a98c8d71ea5445e")
                .addHeader("authtoken", "blt57a01bcc35b524ed")
                .build();
        
        //Execution de la requête
        try (Response responseSearchContentType = client.newCall(requestSearchContentType).execute()) {
            if (responseSearchContentType.code() == 200) {
                return Integer.toString(responseSearchContentType.code());
            } else {
                output = output.concat("<br/> PB Search Content Type il existe peut etre: " + responseSearchContentType.code());
            }
        } catch (Exception e) {
            output = output.concat("<br/> PB Search: " + e);
        }
        return output;
    }

    public static String UpdateContentType(String contentTypeName, String stringContentTypeJson) {
        String output = " ";

        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(stringContentTypeJson, mediaType);

        //Build de la requête
        Request requestUpdateOneEntry = new Request.Builder()
                .url("https://eu-api.contentstack.com/v3/content_types/" + contentTypeName)
                .put(body)
                .addHeader("api_key", "blt0e7212638c9ff7cd")
                .addHeader("authorization", "csa27268198a98c8d71ea5445e")
                .addHeader("Content-Type", "application/json")
                .build();

        //Execution de la requête
        try (Response responseUpdateOneEntry = client.newCall(requestUpdateOneEntry).execute()) {
            if (responseUpdateOneEntry.code() == 200) {
                String responseBody = responseUpdateOneEntry.body().string();
                output = output.concat("<br/> Content type Succesfuly Updated " + contentTypeName);
            } else {
                output = output.concat("<br/> PB Update Content type: " + responseUpdateOneEntry.code());
            }
        } catch (Exception e) {
            output = output.concat("<br/> PB Update Content type: " + e);
        }
        return output;
    }
}
