/*
** ObjectStorageGetObject version 1.0.
**
** Copyright (c) 2020 Oracle, Inc.
** Licensed under the Universal Permissive License v 1.0 as shown at https://oss.oracle.com/licenses/upl.
*/

package com.example.fn;

import com.oracle.bmc.auth.ResourcePrincipalAuthenticationDetailsProvider;
import com.oracle.bmc.objectstorage.ObjectStorage;
import com.oracle.bmc.objectstorage.ObjectStorageClient;
import com.oracle.bmc.objectstorage.requests.GetObjectRequest;
import com.oracle.bmc.objectstorage.responses.GetObjectResponse;
import com.sendgrid.Client;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;



public class ObjectStorageGetObject {

    private ObjectStorage objStoreClient = null;
    final ResourcePrincipalAuthenticationDetailsProvider provider
            = ResourcePrincipalAuthenticationDetailsProvider.builder().build();

    public ObjectStorageGetObject() {

        try {
            //print env vars in Functions container
            System.err.println("OCI_RESOURCE_PRINCIPAL_VERSION " + System.getenv("OCI_RESOURCE_PRINCIPAL_VERSION"));
            System.err.println("OCI_RESOURCE_PRINCIPAL_REGION " + System.getenv("OCI_RESOURCE_PRINCIPAL_REGION"));
            System.err.println("OCI_RESOURCE_PRINCIPAL_RPST " + System.getenv("OCI_RESOURCE_PRINCIPAL_RPST"));
            System.err.println("OCI_RESOURCE_PRINCIPAL_PRIVATE_PEM " + System.getenv("OCI_RESOURCE_PRINCIPAL_PRIVATE_PEM"));

            objStoreClient = new ObjectStorageClient(provider);

        } catch (Throwable ex) {
            System.err.println("Failed to instantiate ObjectStorage client - " + ex.getMessage());
        }
    }

    public static class GetObjectInfo {

        private String bucketName;
        private String name;

        private String accessToken;

        public String getBucketName() {
            return bucketName;
        }

        public void setBucketName(String bucketName) {
            this.bucketName = bucketName;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }



        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }


    }

    public String handle(GetObjectInfo objectInfo) {

        String result = "FAILED";

        if (objStoreClient == null) {
            System.err.println("There was a problem creating the ObjectStorage Client object. Please check logs");
            return result;
        }
        try {

            String nameSpace = System.getenv().get("NAMESPACE");

            GetObjectRequest gor = GetObjectRequest.builder()
                    .namespaceName(nameSpace)
                    .bucketName(objectInfo.getBucketName())
                    .objectName(objectInfo.getName())
                    .build();
            System.err.println("Getting content for object " + objectInfo.getName() + " from bucket " + objectInfo.getBucketName());

            GetObjectResponse response = objStoreClient.getObject(gor);

            result = new BufferedReader(new InputStreamReader(response.getInputStream()))
                    .lines().collect(Collectors.joining("\n"));

            //String stringFromProc = "SONY,20,30,40;LG,1,4,8";
            String[] array1 =  result.split("\n"); // simply use ;
// array1[0] = SONY,20,30,40
// array1[1] = LG,1,4,8

            JSONObject jsonSubObject = null;
            JSONObject jsonFinal = new JSONObject();
            JSONArray jsonArrayRET = new JSONArray();

            for(int i=1;i<array1.length;i++){
                String []array2 = array1[i].split(","); // simply use ,
                // create jsonobjects
                // when i=0 mean for sony and next time i = 1 mean for LG
                jsonSubObject = new JSONObject();
                jsonSubObject.put("id", Integer.parseInt(array2[0]));
                jsonSubObject.put("companyId", Integer.parseInt(array2[1]));
                jsonSubObject.put("distributionListId", Integer.parseInt(array2[2]));
                jsonSubObject.put("firstName", array2[3]);
                jsonSubObject.put("lastName", array2[4]);
                jsonSubObject.put("phoneNumber", array2[5]);
                jsonSubObject.put("emailAddress", array2[6]);
                jsonSubObject.put("schedulingInfo", array2[7]);
                jsonSubObject.put("context", array2[8]);
                // put every object in array
                jsonArrayRET.add(jsonSubObject);
            }
            // finally put array in reported jsonobject
            jsonFinal.put("objects", jsonArrayRET);
            jsonFinal.put("accessToken",objectInfo.getAccessToken());
            //result = jsonFinal.toJSONString();
            result = sendPost(jsonFinal);
//            Client client = new Client();
//
//            Request request = new Request();
//            request.setBaseUri("https://api.astutebot.com");
//            request.setBody("{\"objects\": \" "+ jsonArrayRET + ",\"accessToken\": \"Fa-vvYLm3HTg5PDECxpC0A\"}");
//            request.setMethod(Method.POST);
//            //String param = "param";
//            request.setEndpoint("https://api.astutebot.com/v1/distributionListPerson/bulk");
//
//            try {
//                Response responseP = client.api(request);
//                System.out.println(responseP.getStatusCode());
//                System.out.println(responseP.getBody());
//                System.out.println(responseP.getHeaders());
//            } catch (IOException ex) {
//                throw ex;
//            }

            System.err.println("Finished reading content for object " + objectInfo.getName());

        } catch (Throwable e) {
            System.err.println("Error fetching object " + e.getStackTrace().toString());
            result = "Error fetching object " + e.getStackTrace().toString();
        }

        return result;
    }
    public String  sendPost(JSONObject jo) throws ClientProtocolException, IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();

        HttpPut post = new HttpPut("https://api.astutebot.com/v1/distributionListPerson/bulk");

        // add request parameter, form parameters
        String json =  jo.toString();
//        String json = "{\n" +
//                "\t\"objects\": [{\n" +
//                "\t\t\"firstName\": \"Joe\",\n" +
//                "\t\t\"lastName\": \"Ross\",\n" +
//                "\t\t\"companyId\": 499,\n" +
//                "\t\t\"emailAddress\": \"jross@workforcesoftware.com\",\n" +
//                "\t\t\"phoneNumber\": \"248-285-2821\",\n" +
//                "\t\t\"distributionListId\": 166,\n" +
//                "\t\t\"schedulingInfo\": \"\\\"{\\\"\\\"type\\\"\\\":\\\"\\\"onePerDay\\\"\\\"\",\n" +
//                "\t\t\"context\": \"\\\"\\\"timezone\\\"\\\":\\\"\\\"UTC\\\"\\\"\",\n" +
//                "\t\t\"id\": 151735\n" +
//                "\t}, {\n" +
//                "\t\t\"firstName\": \"Joe\",\n" +
//                "\t\t\"lastName\": \"Ross\",\n" +
//                "\t\t\"companyId\": 499,\n" +
//                "\t\t\"emailAddress\": \"jross@workforcesoftware.com\",\n" +
//                "\t\t\"phoneNumber\": \"248-285-2821\",\n" +
//                "\t\t\"distributionListId\": 166,\n" +
//                "\t\t\"schedulingInfo\": \"\\\"{\\\"\\\"type\\\"\\\":\\\"\\\"onePerDay\\\"\\\"\",\n" +
//                "\t\t\"context\": \"\\\"\\\"timezone\\\"\\\":\\\"\\\"UTC\\\"\\\"\",\n" +
//                "\t\t\"id\": 151736\n" +
//                "\t}],\n" +
//                "\t\"accessToken\": \"Fa-vvYLm3HTg5PDECxpC0A\"\n" +
//                "}";
        StringEntity entity = new StringEntity(json);
        post.setEntity(entity);
        post.setHeader("Accept", "application/json");
        post.setHeader("Content-type", "application/json");

        CloseableHttpResponse response = httpClient.execute(post);
        //System.out.println(EntityUtils.toString(response.getEntity()));

System.out.println(json.toString());
return EntityUtils.toString(response.getEntity());

    }
}
