#include <WiFiS3.h>               // UNO R4 WiFi 专用 WiFi 库 | WiFi library for UNO R4 WiFi
#include "Arduino_LED_Matrix.h"

/************** WiFi AP 设置 **************/
const char ssid[] = "ArduinoAP";   // 热点名称 | Access Point name
const char pass[] = "12345678";    // 热点密码（至少 8 位） | Access Point password (at least 8 characters)

WiFiServer server(23);             // TCP 服务器端口 23 | TCP server on port 23
WiFiClient clients[4];             // 最多支持 4 个客户端 | Support up to 4 clients
int clientCount = 0;

/************** 矩阵LED相关 **************/
ArduinoLEDMatrix matrix;           // 创建矩阵对象 | LED matrix object

/************** 图案定义（12×8 阵列） **************/
// 注意：每行12列，每个元素0=灭，1=亮 | Each row has 12 elements, 0=off, 1=on

uint8_t pattern_RUN[8][12] = {
  {1,1,1,0,1,0,1,0,1,0,1,0},
  {1,0,1,0,1,0,1,0,1,0,1,0},
  {1,1,1,0,1,0,1,0,1,1,1,0},
  {1,0,1,0,1,0,1,0,1,0,1,0},
  {1,0,1,0,1,1,1,0,1,0,1,0},
  {0,0,0,0,0,0,0,0,0,0,0,0},
  {0,0,0,0,0,0,0,0,0,0,0,0},
  {0,0,0,0,0,0,0,0,0,0,0,0}
};

uint8_t pattern_OFF[8][12] = {
  {1,1,1,0,1,1,1,0,1,1,1,0},
  {1,0,1,0,1,0,0,0,1,0,0,0},
  {1,0,1,0,1,1,1,0,1,1,1,0},
  {1,0,1,0,1,0,0,0,1,0,0,0},
  {1,1,1,0,1,0,0,0,1,0,0,0},
  {0,0,0,0,0,0,0,0,0,0,0,0},
  {0,0,0,0,0,0,0,0,0,0,0,0},
  {0,0,0,0,0,0,0,0,0,0,0,0}
};

uint8_t pattern_HEART[8][12] = {
  {0,1,1,0,0,0,0,0,1,1,0,0},
  {1,1,1,1,0,0,0,1,1,1,1,0},
  {1,1,1,1,1,0,1,1,1,1,1,0},
  {0,1,1,1,1,1,1,1,1,1,0,0},
  {0,0,1,1,1,1,1,1,1,0,0,0},
  {0,0,0,1,1,1,1,1,0,0,0,0},
  {0,0,0,0,1,1,1,0,0,0,0,0},
  {0,0,0,0,0,1,0,0,0,0,0,0}
};

uint8_t pattern_SMILE[8][12] = {
  {0,0,1,1,0,0,0,0,1,1,0,0},
  {0,0,1,1,0,0,0,0,1,1,0,0},
  {0,0,0,0,0,0,0,0,0,0,0,0},
  {0,1,0,0,0,0,0,0,0,0,1,0},
  {0,0,1,0,0,0,0,0,0,1,0,0},
  {0,0,0,1,1,1,1,1,1,0,0,0},
  {0,0,0,0,1,1,1,1,0,0,0,0},
  {0,0,0,0,0,0,0,0,0,0,0,0}
};

uint8_t pattern_LEFT[8][12] = {
  {0,0,0,0,1,0,0,0,0,0,0,0},
  {0,0,0,1,1,0,0,0,0,0,0,0},
  {0,0,1,1,1,0,0,0,0,0,0,0},
  {1,1,1,1,1,1,1,1,1,1,1,1},
  {0,0,1,1,1,0,0,0,0,0,0,0},
  {0,0,0,1,1,0,0,0,0,0,0,0},
  {0,0,0,0,1,0,0,0,0,0,0,0},
  {0,0,0,0,0,0,0,0,0,0,0,0}
};

// 修正：pattern_RIGHT 现在是 pattern_LEFT 的完美镜像（左右对称）
uint8_t pattern_RIGHT[8][12] = {
  {0,0,0,0,0,0,0,0,1,0,0,0},
  {0,0,0,0,0,0,0,1,1,0,0,0},
  {0,0,0,0,0,0,0,1,1,1,0,0},
  {0,1,1,1,1,1,1,1,1,1,1,1},
  {0,0,0,0,0,0,0,1,1,1,0,0},
  {0,0,0,0,0,0,0,1,1,0,0,0},
  {0,0,0,0,0,0,0,0,1,0,0,0},
  {0,0,0,0,0,0,0,0,0,0,0,0}
};

/************** L298N 电机引脚定义 **************/
#define ENA 11   // Enable A (PWM for motors 1&2) | 使能A（PWM控制电机1&2）
#define ENB 6   // Enable B (PWM for motors 3&4) | 使能B（PWM控制电机3&4）
#define IN1 10
#define IN2 9
#define IN3 8
#define IN4 7

int motorSpeed = 255; // 电机速度 (0-255) | Motor speed (0-255)
int moveStatus = 0;//0=stop -1=back -2=left -3=right 1=forward

/************** 初始化 **************/
void setup() {
  Serial.begin(9600);
  matrix.begin();           // 矩阵启动 | Start LED matrix
  while (!Serial);

  // 初始化电机控制引脚 | Initialize motor control pins
  pinMode(ENA, OUTPUT);
  pinMode(ENB, OUTPUT);
  pinMode(IN1, OUTPUT);
  pinMode(IN2, OUTPUT);
  pinMode(IN3, OUTPUT);
  pinMode(IN4, OUTPUT);

  stopMotors();

  Serial.println("Starting WiFi Access Point...");  // 正在启动 WiFi 接入点...
  WiFi.beginAP(ssid, pass);

  IPAddress IP = WiFi.localIP();
  Serial.print("AP created, SSID: ");
  Serial.println(ssid);
  Serial.print("IP address: ");
  Serial.println(IP);
  Serial.println("Waiting for TCP clients (port 23)...");  // 等待 TCP 客户端连接（端口23）...

  server.begin();
}

/************** 主循环 **************/
void loop() {
  // 检查新客户端 | Check for new client
  WiFiClient newClient = server.available();
  if (newClient && clientCount < 4) {
    clients[clientCount] = newClient;
    Serial.print("New client connected: ");
    Serial.println(newClient.remoteIP());
    clientCount++;
  }

  // 处理每个客户端的数据 | Process data from each client
  for (int i = 0; i < clientCount; i++) {
    if (clients[i] && clients[i].connected()) {
      if (clients[i].available()) {
        String receivedData = clients[i].readStringUntil('\n');
        receivedData.trim();

        if (receivedData.length() > 0) {
          Serial.print("Received: ");
          Serial.println(receivedData);

          // 回显收到的原始文本（Arduino唯一发送内容）
          clients[i].println("Received: "+receivedData);

          handleCommand(receivedData);
        }
      }
    } else {
      // 客户端断开 | Client disconnected
      if (clients[i]) clients[i].stop();
      Serial.print("Client disconnected: ");
      Serial.println(i);
      for (int j = i; j < clientCount - 1; j++) {
        clients[j] = clients[j + 1];
      }
      clientCount--;
      i--;
    }
  }

  delay(10);
}

/************** 命令解析函数 **************/
void handleCommand(const String &command) {
  String cmd = command;
  cmd.toLowerCase();

  // 新增：处理 speed=xxx 命令
  if (cmd.startsWith("speed=")) {
    String speedStr = cmd.substring(6);
    int newSpeed = speedStr.toInt();
    if (newSpeed >= 0 && newSpeed <= 255) {
      motorSpeed = newSpeed;
      if(moveStatus==-1){
        moveBackward();
      }
      if(moveStatus==1){
        moveForward();
      }
      Serial.print("→ Motor speed set to: ");
      Serial.println(motorSpeed);
    } else {
      Serial.println("Invalid speed value (0-255).");
    }
    return;
  }

  // 提取第一个关键词用于 switch-case
  String keyword = cmd;
  int spacePos = cmd.indexOf(' ');
  if (spacePos >= 0) {
    keyword = cmd.substring(0, spacePos);
  }

  // 使用 switch-case 结构（与原 if 链等效）
  uint8_t cmdKey = 0;
  if (keyword == "forward") cmdKey = 1;
  else if (keyword == "back") cmdKey = 2;
  else if (keyword == "left") cmdKey = 3;
  else if (keyword == "right") cmdKey = 4;
  else if (keyword == "stop") cmdKey = 5;

  switch (cmdKey) {
    case 1:  // forward
      moveForward();
      Serial.println("→ Moving Forward");
      break;
    case 2:  // back
      moveBackward();
      Serial.println("→ Moving Backward");
      break;
    case 3:  // left
      turnLeft();
      delay(500);
      stopMotors();
      Serial.println("→ Turning Left");
      break;
    case 4:  // right
      turnRight();
      delay(500);
      stopMotors();
      Serial.println("→ Turning Right");
      break;
    case 5:  // stop
      stopMotors();
      Serial.println("→ Stopped");
      break;
    default:
      Serial.println("Unknown command.");
      break;
  }
}

/************** 电机控制函数 **************/
void moveForward() {
  moveStatus=1;
  analogWrite(ENA, motorSpeed);
  analogWrite(ENB, motorSpeed);
  digitalWrite(IN1, HIGH);
  digitalWrite(IN2, LOW);
  digitalWrite(IN3, HIGH);
  digitalWrite(IN4, LOW);
  displayPattern("run");
}

void moveBackward() {
  moveStatus=-1;
  analogWrite(ENA, motorSpeed);
  analogWrite(ENB, motorSpeed);
  digitalWrite(IN1, LOW);
  digitalWrite(IN2, HIGH);
  digitalWrite(IN3, LOW);
  digitalWrite(IN4, HIGH);
  displayPattern("smile");
}

void turnLeft() {
  moveStatus=2;
  analogWrite(ENA, motorSpeed);
  analogWrite(ENB, motorSpeed);
  digitalWrite(IN1, LOW);
  digitalWrite(IN2, HIGH);
  digitalWrite(IN3, HIGH);
  digitalWrite(IN4, LOW);
  displayPattern("leftarrow");
}

void turnRight() {
  moveStatus=3;
  analogWrite(ENA, motorSpeed);
  analogWrite(ENB, motorSpeed);
  digitalWrite(IN1, HIGH);
  digitalWrite(IN2, LOW);
  digitalWrite(IN3, LOW);
  digitalWrite(IN4, HIGH);
  displayPattern("rightarrow");
}

void stopMotors() {
  moveStatus=0;
  analogWrite(ENA, 0);
  analogWrite(ENB, 0);
  digitalWrite(IN1, LOW);
  digitalWrite(IN2, LOW);
  digitalWrite(IN3, LOW);
  digitalWrite(IN4, LOW);
  displayPattern("off");
}

/************** 显示函数 **************/
void displayPattern(String name) {
  name.toLowerCase();
  matrix.clear();

  if (name == "run") {
    matrix.renderBitmap(pattern_RUN, 12, 8);
  } else if (name == "off") {
    matrix.renderBitmap(pattern_OFF, 12, 8);
  } else if (name == "heart") {
    matrix.renderBitmap(pattern_HEART, 12, 8);
  } else if (name == "smile") {
    matrix.renderBitmap(pattern_SMILE, 12, 8);
  } else if (name == "leftarrow") {
    matrix.renderBitmap(pattern_LEFT, 12, 8);
  } else if (name == "rightarrow") {
    matrix.renderBitmap(pattern_RIGHT, 12, 8);
  } else {
    matrix.clear();
  }
}