package com.function;

import java.io.IOException;
import java.util.ArrayList;
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


        String requestBody = request.getBody().orElse(null);
        Object obj = new JSONParser().parse(requestBody);
        JSONObject json = (JSONObject) obj;
        System.out.println("all:" + json);
        JSONObject jsonData = (JSONObject) json.get("params");
        System.out.println(jsonData);
        JSONArray array = (JSONArray) jsonData.get("entry");
        JSONObject jsonObj = (JSONObject) array.get(0);
        String contentTypeName = (String) jsonObj.get("Business component");
        Set<String> keys = jsonObj.keySet();
        String output = " ";

        for (String title : keys) {
            if (!(title.equals("Short description keyID")
                    || title.equals("Business component")
                    || title.equals("Key"))) {
                String langue = title;
                System.out.println(langue);
                JSONObject content = new JSONObject();
                content.put("title", "import_" + langue);

                List<String> listFieldsContentType = new ArrayList<String>();

                for (Object elementArray : array) {
                    JSONObject elementJson = (JSONObject) elementArray;
                    String key = (String) elementJson.get("Key");
                    String value = (String) elementJson.get(langue);
                    content.put(key, value);
                    listFieldsContentType.add(key);
                }
                System.out.println(content.toString());
                JSONObject contentAll = new JSONObject();
                contentAll.put("entry", content);
                String uid = Search("import_" + langue, contentTypeName);
                
                String stringBodyJson=genrateContentTypeJSONFromListOfFields(contentTypeName,listFieldsContentType); 
                String resCreateContentType = CreateContentType(stringBodyJson);
                output = output.concat(resCreateContentType);
                
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

    public String Search(String name, String contentTypeName) {
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");
        Request requestSearchEntry = new Request.Builder()
                .url("https://eu-api.contentstack.com/v3/content_types/" + contentTypeName+ "/entries?query={\"title\":\"" + name +"\"}")
                .get()
                .addHeader("api_key", "blt0e7212638c9ff7cd")
                .addHeader("access_token", "csa27268198a98c8d71ea5445e")
                .addHeader("Content-Type", "application/json")
                .addHeader("authtoken", "blt57a01bcc35b524ed")
                .build();
        String output = " ";
        try (Response responseSearchEntry = client.newCall(requestSearchEntry).execute()) {
            if (responseSearchEntry.code() == 200) {
                String responseBody = responseSearchEntry.body().string();
                Object obj = new JSONParser().parse(responseBody);
                JSONObject json = (JSONObject) obj;
                JSONArray array = (JSONArray) json.get("entries");
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

    public String UpdateEntry(String contentTypeName, String uid, String langue, JSONObject contentAll) {
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(contentAll.toJSONString(), mediaType);
        Request requestUpdateOneEntry = new Request.Builder()
                .url("https://eu-api.contentstack.com/v3/content_types/" + contentTypeName + "/entries/" + uid+ "?locale=" + langue)
                .put(body)
                .addHeader("api_key", "blt0e7212638c9ff7cd")
                .addHeader("authorization", "csa27268198a98c8d71ea5445e")
                .addHeader("Content-Type", "application/json")
                .build();
        String output = " ";

        try (Response responseUpdateOneEntry = client.newCall(requestUpdateOneEntry).execute()) {
            if (responseUpdateOneEntry.code() == 200) {
                String responseBody = responseUpdateOneEntry.body().string();
                output = output.concat("<br/> Succesfuly Updated " + uid);
            } else {
                output = output.concat("<br/> PB Update: " + responseUpdateOneEntry.code());
                System.out.println(output);
            }
        } catch (Exception e) {
            output = output.concat("<br/> PB Update: " + e);
            System.out.println(output);
        }
        return output;
    }

    public String CreateEntry(String contentTypeName, String uid, String langue, JSONObject contentAll) {
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");
        String output = "";
        RequestBody body = RequestBody.create(contentAll.toJSONString(), mediaType);
        Request requestCreateOneEntry = new Request.Builder()
                .url("https://eu-api.contentstack.com/v3/content_types/" + contentTypeName + "/entries?locale="+ langue)
                .post(body)
                .addHeader("api_key", "blt0e7212638c9ff7cd")
                .addHeader("authorization", "csa27268198a98c8d71ea5445e")
                .addHeader("Content-Type", "application/json")
                .build();
        try (Response responseCreateOneEntry = client.newCall(requestCreateOneEntry).execute()) {
            // System.out.println("response a analyser"+response.toString());
            if (responseCreateOneEntry.code() == 201) {
                String responseBody = responseCreateOneEntry.body().string();
                Object objectReponsObject = new JSONParser().parse(responseBody);
                JSONObject jsonReponse = (JSONObject) objectReponsObject;
                JSONObject entryData = (JSONObject) jsonReponse.get("entry");
                String createdUid = (String) entryData.get("uid");
                output = output.concat("<br/> Succefully Created " + createdUid);
                System.out.println(output);
            } else {
                output = output.concat("<br/> PB requete de creation d'une entry, le Code retour: " + responseCreateOneEntry.code());
                System.out.println(output);
            }

        } catch (Exception e) {
            output = output.concat("<br/> PB avec l'execution de la requete de creation d'une entry");
            System.out.println(output);
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
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(stringContentTypeJson,mediaType);
        String output = "";
        Request createContentType = new Request.Builder()
           .url("https://eu-api.contentstack.com/v3/content_types/")
           //.method("POST", body)  Seems to be deprecated
           .post(body)
           .addHeader("api_key", "blt0e7212638c9ff7cd")
           .addHeader("authorization", "csa27268198a98c8d71ea5445e")
           .addHeader("Content-Type", "application/json")
           .build();
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



     public String SearchContentType(String contentTypeName) {
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");
        Request requestSearchContentType= new Request.Builder()
                .url("https://eu-api.contentstack.com/v3/content_types/" + contentTypeName+ "\"}")
                .get()
                .addHeader("api_key", "blt02f7b45378b008ee")
                .addHeader("access_token", "cs5b69faf35efdebd91d08bcf4")
                .build();
        String output = " ";
        try (Response responseSearchContentType = client.newCall(requestSearchContentType).execute()) {
            if (responseSearchContentType.code() == 200) {
                String responseBody = responseSearchContentType.body().string();
                Object obj = new JSONParser().parse(responseBody);
                JSONObject json = (JSONObject) obj;
                JSONArray array = (JSONArray) json.get("content_type");
                if (array.size() == 0) {
                    output = "";
                } else {
                    JSONObject jsonObj = (JSONObject) array.get(0);
                    String uid = (String) jsonObj.get("uid");
                    output = uid;
                }
            } else {

                output = output.concat("<br/> PB Search Content Type: " + responseSearchContentType.code());
                System.out.println(output);
            }
        } catch (Exception e) {
            output = output.concat("<br/> PB Search: " + e);
            System.out.println(output);
        }
        return output;
    }

}
