/*
 * Copyright (c) 2016 Onegini B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.onegini.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.json.JSONException;
import org.json.JSONObject;

import android.support.annotation.NonNull;
import retrofit.client.Header;
import retrofit.client.Response;

public class RetrofitResponseUtil {

  public static JSONObject getJsonHeadersFromRetrofitResponse(Response response) throws JSONException {
    final JSONObject result = new JSONObject();

    for (Header header : response.getHeaders()) {
      result.put(header.getName(), header.getValue());
    }

    return result;
  }

  @NonNull
  public static String getBodyStringFromRetrofitResponse(Response response) {
    final BufferedReader reader;
    final StringBuilder stringBuilder = new StringBuilder();

    if (response.getBody() == null) {
      return "";
    }

    try {
      reader = new BufferedReader(new InputStreamReader(response.getBody().in()));
      String line;

      try {
        while ((line = reader.readLine()) != null) {
          stringBuilder.append(line);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }


    return stringBuilder.toString();
  }
}
