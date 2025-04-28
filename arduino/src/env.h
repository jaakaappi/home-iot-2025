#include "Arduino.h"

const char *ssid = "your_ssid";
const char *password = "your_password";

String aws_access_key = "YOUR_ACCESS_KEY";
String aws_secret_key = "YOUR_SECRET_KEY";
String aws_region = "eu-west-1";
String service = "lambda";
String host = "lambda.us-east-1.amazonaws.com";
String canonical_uri = "/2015-03-31/functions/my-function-name/invocations";
String content_type = "application/json";