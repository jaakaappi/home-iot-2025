#include <WiFi.h>
#include <HTTPClient.h>
#include "env.h"
#include <DHT.h>
#include <DHT_U.h>
#include <base64.hpp>
#include "Adafruit_VEML7700.h"

Adafruit_VEML7700 veml = Adafruit_VEML7700();

#define DHTTYPE DHT22
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

#define IRRIGATION_DURATION_MS 100
#define PUMP_PIN GPIO_NUM_26
#define BUTTON_IN GPIO_NUM_32

void irrigate()
{
  Serial.println("Starting irrigation");
  digitalWrite(PUMP_PIN, HIGH);
  delay(IRRIGATION_DURATION_MS);
  digitalWrite(PUMP_PIN, LOW);
  Serial.println("Stopped irrigation");
}

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

    http.begin(client, host + "/data");

    http.addHeader("Authorization", authHeader);
    String data = String(relativeHumidity) + " " + String(temperature) + " " + String(lux) + " " +
                  String(soilMoisture1) + " " + String(soilMoisture2) + " " + String(soilMoisture3);
    Serial.println(data);
    int httpResponseCode = http.POST(data);
    String httpResponse = http.getString();

    Serial.print("HTTP Response code: ");
    Serial.println(httpResponseCode);

    Serial.print("HTTP Response: ");
    Serial.println(httpResponse);

    http.end();
    client.stop();

    if (httpResponse == "I")
    {
      irrigate();
    }
  }
}

void checkManualIrrigation()
{
  if (digitalRead(BUTTON_IN) == HIGH)
  {
    irrigate();

    WiFiClient client;
    HTTPClient http;

    http.begin(client, host + "/irrigation");

    http.addHeader("Authorization", authHeader);
    int httpResponseCode = http.POST("");
    String httpResponse = http.getString();

    Serial.print("HTTP Response code: ");
    Serial.println(httpResponseCode);

    Serial.print("HTTP Response: ");
    Serial.println(httpResponse);

    http.end();
    client.stop();
  }
}

void setup()
{
  Serial.begin(115200);

  delay(1000);

  pinMode(PUMP_PIN, OUTPUT);
  digitalWrite(PUMP_PIN, LOW);
  pinMode(BUTTON_IN, INPUT_PULLDOWN);

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

  delay(1000);
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
  checkManualIrrigation();
  delay(1000);
}
