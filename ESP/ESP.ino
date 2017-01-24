/*
  ESP-8266 that connects to a WiFi network and chats with an MQTT server (publish & subscribe)
*/
#include <ArduinoJson.h>          // JSON library
#include <ESP8266WiFi.h>          // ESP8266 library
#include <PubSubClient.h>         // MQTT library
#include <DNSServer.h>            // Local DNS Server used for redirecting all requests to the configuration portal
#include <ESP8266WebServer.h>     // Local WebServer used to serve the configuration portal
#include <WiFiManager.h>          // https://github.com/tzapu/WiFiManager WiFi Configuration Magics

// Variables
float vcc;                             // Battery level
ADC_MODE(ADC_VCC);
const String application = "car";  // Name of the application, used in the log MQTT payloads and topics

// WiFi setup
const String ssid = "ESP_" + application;
const char* password = "";
const char* client_mac;

// MQTT setup
const char* mqtt_server = "<IP>";
const int mqtt_port = 1883;
const char* mqtt_username = "esp";                  // Used while connecting to the broker
const char* mqtt_password = "";                     // Leave empty if not needed
const String mqtt_topic_syntax = "home/";           // The first part of the MQTT topic, the second part is generated with the application name
const char* mqtt_topic = "";
const char* mqtt_topic_log = "home/log";            // Used to log the hardware status to broker
char* mqtt_payload;
const boolean retained = false;

// Motor setup
const int mFR_1 = 0;
const int mFR_2 = 2;
const int mFL_1 = 12;
const int mFL_2 = 14;
const int mBR_1 = 4;
const int mBR_2 = 5;
const int mBL_1 = 13;
const int mBL_2 = 15;

WiFiClient espClient;
PubSubClient client(espClient);

void setup() {
  // Setup serial port
  Serial.begin(115200);

  // Setup WiFi
  WiFiManager wifiManager;
  // Reset saved settings
  //wifiManager.resetSettings();
  wifiManager.autoConnect(strToChar(ssid), password);

  client.setServer(mqtt_server, mqtt_port);
  client.setCallback(callback);

  Serial.println("Application name: " + application);
  Serial.println("MQTT Server: " + String(mqtt_server));

  client_mac = client_MAC();
  Serial.println("ESP mac: " + String(client_mac));

  // Create mqtt_topic with application name
  mqtt_topic = createMqtt_topic(mqtt_topic_syntax);
  Serial.println("MQTT Topic: " + String(mqtt_topic));

  // Send MQTT connection status to broker
  connect_MQTT();

  // Setup motors
  pinMode(mFR_1, OUTPUT);
  pinMode(mFR_2, OUTPUT);
  pinMode(mFL_1, OUTPUT);
  pinMode(mFL_2, OUTPUT);
  pinMode(mBR_1, OUTPUT);
  pinMode(mBR_2, OUTPUT);
  pinMode(mBL_1, OUTPUT);
  pinMode(mBL_2, OUTPUT);

  analogWrite(mFR_1, 0);
  analogWrite(mFR_2, 0);
  analogWrite(mFL_1, 0);
  analogWrite(mFL_2, 0);
  analogWrite(mBR_1, 0);
  analogWrite(mBR_2, 0);
  analogWrite(mBL_1, 0);
  analogWrite(mBL_2, 0);

  //digitalWrite(LED_BUILTIN, HIGH);   // Turn the LED off (Note that HIGH is the voltage level
}

void loop() {
  if (!client.connected()) {
    connect_MQTT();
  }
  client.loop();
}

