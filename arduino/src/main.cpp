#include <WiFi.h>
#include <HTTPClient.h>
#include "env.h"
#include <DHT.h>
#include <DHT_U.h>
#include "Adafruit_VEML7700.h"

Adafruit_VEML7700 veml = Adafruit_VEML7700();

#define DHTTYPE DHT22
#define DHTPIN GPIO_NUM_27

DHT_Unified dht(DHTPIN, DHTTYPE);

#define SOIL_MOISTURE_1_OUT_PIN GPIO_NUM_33 // resistive
#define SOIL_MOISTURE_1_PIN GPIO_NUM_36     // resistive
#define SOIL_MOISTURE_2_PIN GPIO_NUM_39     // pcb
#define SOIL_MOISTURE_3_PIN GPIO_NUM_34     // metal

#define IRRIGATION_DURATION_MS 10 * 1000
#define PUMP_PIN GPIO_NUM_32
#define BUTTON_IN GPIO_NUM_26

#define AVERAGING_COUNT 100

void irrigate()
{
  Serial.println("Starting irrigation");
  digitalWrite(PUMP_PIN, HIGH);
  delay(IRRIGATION_DURATION_MS);
  digitalWrite(PUMP_PIN, LOW);
  Serial.println("Stopped irrigation");
}

float takeAveragedMeasurement(int pin)
{
  float reading = 0.0;

  for (int i = 0; i < AVERAGING_COUNT; i++)
  {
    reading += analogRead(pin);
    delay(10);
  }

  return reading / AVERAGING_COUNT;
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

  digitalWrite(SOIL_MOISTURE_1_OUT_PIN, HIGH);
  delay(100);
  float soilMoisture1 = takeAveragedMeasurement(SOIL_MOISTURE_1_PIN);
  digitalWrite(SOIL_MOISTURE_1_OUT_PIN, LOW);
  Serial.println(soilMoisture1);
  digitalWrite(SOIL_MOISTURE_1_OUT_PIN, LOW);

  float soilMoisture2 = takeAveragedMeasurement(SOIL_MOISTURE_2_PIN);
  Serial.println(soilMoisture2);
  float soilMoisture3 = takeAveragedMeasurement(SOIL_MOISTURE_3_PIN);
  Serial.println(soilMoisture3);

  if (isnan(temperature) || isnan(relativeHumidity) || isnan(lux))
  {
    Serial.println("Failed to read DHT22 or VEML7700");
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
  pinMode(SOIL_MOISTURE_1_OUT_PIN, OUTPUT);
  digitalWrite(SOIL_MOISTURE_1_OUT_PIN, LOW);
  pinMode(PUMP_PIN, OUTPUT);
  digitalWrite(PUMP_PIN, LOW);
  pinMode(BUTTON_IN, INPUT_PULLDOWN);
  pinMode(SOIL_MOISTURE_1_PIN, INPUT_PULLDOWN);
  pinMode(SOIL_MOISTURE_2_PIN, INPUT_PULLDOWN);
  pinMode(SOIL_MOISTURE_3_PIN, INPUT_PULLDOWN);

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
