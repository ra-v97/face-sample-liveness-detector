# Face Sample Liveness Detector

FSLD is a research application developed for devices running on the Android operating system, the main purpose of which is to verify methods for detecting the viability of face samples as part of a mobile authentication system.

The application is developed as a project within the Mobile Systems course at the AGH University of Science and Technology in Krakow.

## Project characteristics

The application provides an interface to access the camera and allows to dynamically change the algorithm of sample viability verification. Currently three methods have been implemented:
- use of the occurrence of natural reflections formed on the iris of the human eye after exposure to flash during taking the photo;
- observation of facial movements such as eyelid blinking, head turning or smiling;
- task mode that forces the user to perform tasks involving a specific sequence of facial movements.

Using the API it is possible to easily implement subsequent algorithms and analyze data about the effectiveness of the solution, which are collected and stored in the memory of the mobile device.

## Installation

```
1. Download repository
2. Import projet to Android Studio
3. Install application and run on simulator or real Android Device
```
