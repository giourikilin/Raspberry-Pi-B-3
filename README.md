# Raspberry-Pi-B-3-Camera-App-

Object Detection App For Raspberry Pi

This is an experimental Android application for the Raspberry Pi 3 B+.

The main goal was to develop a camera application controlled using gestures with the help of a computer vision algorithm. 
To locate in real time and classify each of three gestures SSD-MobilenetV2 was used. 
It is a fast and fairly accurate object detector made specifically for devices with low computational power. 
The three categories of gestures are a) fist, b) open hand (palm facing the camera and fingers closed) and c) the sign of victory/peace. 
Based on the detected gesture specific commands execute such as capturing and saving photos, viewing captured photos and
browsing among saved pictures. To achieve those functionalities a custom data set was created, 180 images in total for three gesture categories. 
Each category includes 60 images, for each image was created a XML file that contains information about the image.
Information such as coordinates of the hand and gesture label. Tensorflow Object Detection API was used for transfer-learning locally on a laptop. 
The trained model was exported as a tflite file to be used in an Android environment. 
Finally the application was developed using Java programming language and Android Studio, the official Integrated Development Environment (IDE) for Android applications.

*The app is not perfect but it works. 

Parts that need to be improved are:

1) Some times the model is not loaded when launching the app. So a device restart is needed. I haven't figured out yet why this is happening.
Maybe a problem wtih the OS initialization.

2) Develop a better approach to compensate the rotation of the frames fed to the Object Detection Model for proccessing.  
