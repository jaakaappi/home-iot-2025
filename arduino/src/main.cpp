#include <WiFi.h>
#include <HTTPClient.h>
#include "env.h"

void invokeLambda()
{
  WiFiClient client;
  HTTPClient http;

  http.begin(client, host);

  http.addHeader("Content-Type", "application/json", "X-API-Key", apiKey);
  http.setFollowRedirects(HTTPC_FORCE_FOLLOW_REDIRECTS);
  int httpResponseCode = http.POST("{\"h\":" + (String)0.0 + ",\"t\":" + (String)0.0 + ",\"l\":" + (String)0.0 + ",\"h1\":" + (String)0.0 + ",\"h2\":" + (String)0.0 + ",\"h3\":" + (String)0.0 + "}");

  Serial.print("HTTP Response code: ");
  Serial.println(httpResponseCode);

  Serial.print("HTTP Response: ");
  Serial.println(http.getString());

  http.end();
  client.stop();
}

void setup()
{
  Serial.begin(115200);

  delay(1000);

  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED)
  {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nWiFi connected");

  invokeLambda();
}

void loop()
{
  // Nothing
}
