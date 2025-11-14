# Android–Arduino Connector App
This is an Android application designed to connect to Arduino devices. The UI mimics the layout and interaction style of modern social media apps, providing a simple and friendly interface for communication between Android devices and Arduino hardware.
## 📦 Manual Deployment Guide
### 1. Download Project Source Code
Download the source code from the Release page of this repository.
### 2. Download the Vosk Speech Recognition Model
Required model: vosk-model-small-en-us-0.15  
Official model page: https://alphacephei.com/vosk/models  
Direct download link: https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip
### 3. Prepare the Vosk Model Folder
Unzip vosk-model-small-en-us-0.15.zip and rename the extracted folder to "model"
Ensure the folder contains:
am/
conf/
graph/
ivector/
### 4. Place the Model in the Project Assets
Move the model folder to:
AndroidToArduinoOr/app/src/main/assets/
Final structure:
app/src/main/assets/model/...
### 5. Open and Build the Project
Open the project using Android Studio and compile it. 
Core source code directory:
AndroidToArduinoOr/app/src/main/java/com/example/androidtoarduinoor/

# 中文说明 (Chinese Version)
这是一个用于连接 Arduino 设备的安卓应用程序。应用界面模仿社交媒体应用的设计风格，旨在提供更简便和直观的方式让安卓设备连接 Arduino。
## 📦 手动部署方法
### 1. 下载项目源码
从本仓库的 Release 页面下载源代码。
### 2. 下载 Vosk 语音识别模型
所需模型：vosk-model-small-en-us-0.15  
官方页面：https://alphacephei.com/vosk/models  
官方下载地址：https://alphacephei.com/vosk/models/vosk-model-small-en-us-0.15.zip
### 3. 准备模型文件夹
解压 vosk-model-small-en-us-0.15.zip 并重命名为："model"
确认目录结构包含：
am/
conf/
graph/
ivector/
### 4. 将模型放入项目 assets 文件夹
放置到：
AndroidToArduinoOr/app/src/main/assets/
### 5. 打开并编译项目
使用 Android Studio 打开并编译。
核心代码目录：AndroidToArduinoOr/app/src/main/java/com/example/androidtoarduinoor/

