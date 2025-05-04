#include <WiFi.h>
#include <HTTPClient.h>
#include "env.h"
#include <DHT.h>
#include <DHT_U.h>
#include <base64.hpp>
#include "Adafruit_VEML7700.h"

Adafruit_VEML7700 veml = Adafruit_VEML7700();

#define DHTTYPE DHT11
#define DHTPIN GPIO_NUM_27

DHT_Unified dht(DHTPIN, DHTTYPE);

#define SOIL_MOISTURE_1_PIN GPIO_NUM_33 // resistive
#define SOIL_MOISTURE_2_PIN GPIO_NUM_35 // pcb
#define SOIL_MOISTURE_3_PIN GPIO_NUM_39 // metal
#define SOIL_MOISTURE_1_HIGH 1488.0
#define SOIL_MOISTURE_2_HIGH 3270.0
#define SOIL_MOISTURE_3_HIGH 4095.0
#define SOIL_MOISTURE_1_LOW 0.0
#define SOIL_MOISTURE_2_LOW 1295.0
#define SOIL_MOISTURE_3_LOW 1363.0

void sendMeasurement()
{
  dht.begin();
  sensor_t sensor;
  sensors_event_t event;

  float temperature = 0.0;
  float relativeHumidity = 0.0;

  dht.temperature().getEvent(&event);
  temperature = event.temperature;
  dht.humidity().getEvent(&event);
  relativeHumidity = event.relative_humidity;

  float lux = veml.readLux();

  // float soilMoisture1 = (1.0 - (analogRead(SOIL_MOISTURE_1_PIN) - SOIL_MOISTURE_1_LOW) / (SOIL_MOISTURE_1_HIGH - SOIL_MOISTURE_1_LOW)) * 100.0;
  float soilMoisture1 = 0.0;
  float soilMoisture2 = (1.0 - (analogRead(SOIL_MOISTURE_2_PIN) - SOIL_MOISTURE_2_LOW) / (SOIL_MOISTURE_2_HIGH - SOIL_MOISTURE_2_LOW)) * 100.0;
  float soilMoisture3 = (1.0 - (analogRead(SOIL_MOISTURE_3_PIN) - SOIL_MOISTURE_3_LOW) / (SOIL_MOISTURE_3_HIGH - SOIL_MOISTURE_3_LOW)) * 100.0;

  soilMoisture1 = soilMoisture1 < 0 ? 0 : soilMoisture1 > 100 ? 100
                                                              : soilMoisture1;
  soilMoisture2 = soilMoisture2 < 0 ? 0 : soilMoisture2 > 100 ? 100
                                                              : soilMoisture2;
  soilMoisture3 = soilMoisture3 < 0 ? 0 : soilMoisture3 > 100 ? 100
                                                              : soilMoisture3;

  if (isnan(temperature) || isnan(relativeHumidity) || isnan(lux))
  {
    Serial.println("Failed to read DHT11 or VEML7700");
    Serial.print("Temp");
    Serial.println(temperature);
    Serial.print("Hum");
    Serial.println(relativeHumidity);
    Serial.print("Lux");
    Serial.println(lux);
  }
  else
  {
    WiFiClient client;
    HTTPClient http;

    http.begin(client, host);

    http.addHeader("Authorization", authHeader);
    String data = String(relativeHumidity) + " " + String(temperature) + " " + String(lux) + " " +
                  String(soilMoisture1) + " " + String(soilMoisture2) + " " + String(soilMoisture3);
    Serial.println(data);
    int httpResponseCode = http.POST(data);

    Serial.print("HTTP Response code: ");
    Serial.println(httpResponseCode);

    Serial.print("HTTP Response: ");
    Serial.println(http.getString());

    http.end();
    client.stop();
  }
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

  if (!veml.begin())
  {
    Serial.println("VEML7700 not found");
    while (1)
      ;
  }
  Serial.println("VEML7700 found");

  Serial.print(F("Gain: "));
  switch (veml.getGain())
  {
  case VEML7700_GAIN_1:
    Serial.println("1");
    break;
  case VEML7700_GAIN_2:
    Serial.println("2");
    break;
  case VEML7700_GAIN_1_4:
    Serial.println("1/4");
    break;
  case VEML7700_GAIN_1_8:
    Serial.println("1/8");
    break;
  }

  Serial.print(F("Integration Time (ms): "));
  switch (veml.getIntegrationTime())
  {
  case VEML7700_IT_25MS:
    Serial.println("25");
    break;
  case VEML7700_IT_50MS:
    Serial.println("50");
    break;
  case VEML7700_IT_100MS:
    Serial.println("100");
    break;
  case VEML7700_IT_200MS:
    Serial.println("200");
    break;
  case VEML7700_IT_400MS:
    Serial.println("400");
    break;
  case VEML7700_IT_800MS:
    Serial.println("800");
    break;
  }

  Serial.println("Soil moisture 1 ");
  Serial.println(analogRead(SOIL_MOISTURE_1_PIN));
  Serial.println("Soil moisture 2 ");
  Serial.println(analogRead(SOIL_MOISTURE_2_PIN));
  Serial.println("Soil moisture 3 ");
  Serial.println(analogRead(SOIL_MOISTURE_3_PIN));

  sendMeasurement();
}

int startMillis = 0;

void loop()
{
  if (millis() - startMillis >= 10 * 60 * 1000)
  {
    sendMeasurement();
    startMillis = millis();
  }
  delay(10 * 1000);
}
