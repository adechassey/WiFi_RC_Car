/********** MQTT functions **********/

const char* createMqtt_topic(String mqtt_topic_syntax) {
  mqtt_topic_syntax += application;
  return strToChar(mqtt_topic_syntax);
}

void connect_MQTT() {
  // Loop until we're reconnected
  while (!client.connected()) {
    Serial.print("Attempting MQTT connection... ");
    // Attempting to connect
    if (client.connect(client_mac, mqtt_username, mqtt_password, mqtt_topic_log, 1, 1, strToChar("{\"name\": \"" + application + "\", \"status\": \"disconnected\"}"))) {
      Serial.println("\t/!\\ connected /!\\");
      // Sending log "connected" to broker
      //client.publish(mqtt_topic_log, strToChar("{\"name\": \"" + application + "\", \"status\": \"connected\"}"), retained);
      send_MQTT_log(ESP.getVcc());
      client.subscribe(mqtt_topic, 1);
    } else {
      Serial.print("failed, rc=");
      Serial.print(client.state());
      Serial.println(" try again in 5 seconds");
      // Wait 5 seconds before retrying
      delay(5000);
    }
  }
}

void send_MQTT_log(float vcc) {
  mqtt_payload = strToChar("{\"name\": \"" + application + "\", \"status\": \"connected\", \"battery\": " + String(vcc) + "}");
  client.publish(mqtt_topic_log, mqtt_payload , retained);
}


// handles message arrived on subscribed topic(s)
void callback(char* topic, byte* payload, unsigned int length) {
  char json[150];
  Serial.print("Message arrived [");
  Serial.print(topic);
  Serial.print("] ");
  for (int i = 0; i < length; i++) {
    Serial.print((char)payload[i]);
    json[i] = (char)payload[i];
  }
  Serial.println();
  Serial.println(json);

  StaticJsonBuffer<200> jsonBuffer;
  JsonObject& root = jsonBuffer.parseObject(json);
  if (!root.success()) {
    Serial.println("parseObject() failed");
    return;
  }
  String action = root["action"];
  int thrust = root["thrust"];
  if (action == "forward")
    forward(thrust);
  if (action == "backward")
    backward(thrust);
  if (action == "left")
    left(thrust);
  if (action == "right")
    right(thrust);
  Serial.println(action);
  Serial.println(thrust);
}

