// More http://www.himix.lt/arduino/arduino-and-color-recognition-sensor-tcs230-tcs3200/
#include <SoftwareSerial.h>
//Include the Arduino Stepper Library
#include <Stepper.h>

SoftwareSerial bluetooth(10, 11); //// RX,TX of Virtual and TX, RX of Arduino ....pin 10 of arduino to tx of hc05
#define S0 4
#define S1 5
#define S2 6
#define S3 7
#define sensorOut 8
int frequency = 0, red = 0, green = 0, blue = 0;
int start;
// Number of steps per internal motor revolution
const float STEPS_PER_REV = 32;
//  Amount of Gear Reduction
const float GEAR_RED = 64;
// Number of steps per geared output rotation
const float STEPS_PER_OUT_REV = STEPS_PER_REV * GEAR_RED;
// Number of Steps Required
int StepsRequired;
// Create Instance of Stepper Class
// Specify Pins used for motor coils
// The pins used are 2,3,12,13
// Connected to ULN2003 Motor Driver In1, In2, In3, In4
// Pins entered in sequence 1-3-2-4 for proper step sequencing

Stepper steppermotor(STEPS_PER_REV, 2, 12, 3, 13);

String rgb;
void setup() {
  pinMode(S0, OUTPUT);
  pinMode(S1, OUTPUT);
  pinMode(S2, OUTPUT);
  pinMode(S3, OUTPUT);
  pinMode(sensorOut, INPUT);
  // S2 and S3 are used to detect color freq.
  // S0 and S1 are used for scaling of frequency
  // Setting frequency-scaling to 20% (its common to use this in arduino)
  digitalWrite(S0, HIGH);
  digitalWrite(S1, LOW);

  Serial.begin(115200);
  bluetooth.begin(9600);
}
void loop() {
  if (bluetooth.available() > 0 || start) {
    if (bluetooth.available() > 0) {
      start = bluetooth.read();
      if (start == 0)
      {
        Serial.print("Device Status : ");
        Serial.print(start);
        return false;
      }
    }
    Serial.print("Device Status : ");
    Serial.print(start);
    Serial.println();
    delay(1000);
    Serial.print("Inside ");
    // Setting red filtered photodiodes to be read
    digitalWrite(S2, LOW);
    digitalWrite(S3, LOW);

    //earlier we used -> frequency = pulseIn(sensorOut, LOW);
    frequency = pulseIn(sensorOut, digitalRead(sensorOut) == HIGH ? LOW : HIGH);  // Reading the output frequency
    // 50% duty cycle so high and low same, to read next high/low above second parameter is written (not necessary)
    //what we are reading is not freqency but it is period in microseconds. So f=1/T which is done in formula.
    /*some sites uses ->
       frequency = map(frequency, 25,72,0,255);
       === to Remaping the value of the frequency to the RGB Model of 0 to 255
    */
    red = 10200 / frequency; // calibrate using white color
    // red = 8415 / frequency; old calibration
    red = red > 255 ? 255 : red;
    // Serial.print("R= ");
    //  bluetooth.print(red);//printing RED color frequency
    Serial.print(red);
    Serial.print("  ");

    delay(200);
    //#########################################################
    // Setting Green filtered photodiodes to be read
    digitalWrite(S2, HIGH);
    digitalWrite(S3, HIGH);

    //earlier we used -> frequency = pulseIn(sensorOut, LOW);
    frequency = pulseIn(sensorOut, digitalRead(sensorOut) == HIGH ? LOW : HIGH);  // Reading the output frequency
    /*some sites uses ->
      frequency = map(frequency, 30,90,0,255);
      === to Remaping the value of the frequency to the RGB Model of 0 to 255
    */

    green = 13005 / frequency; // calibration formula= white period that is read from sensor*255/current
    //  green = 9180 / frequency; old calibration
    green = green > 255 ? 255 : green;
    //   Serial.print("G= ");
    //   bluetooth.print(green);//printing Green color frequency
    Serial.print(green);
    Serial.print("  ");

    delay(200);
    //#########################################################
    // Setting Blue filtered photodiodes to be read
    digitalWrite(S2, LOW);
    digitalWrite(S3, HIGH);

    //earlier we used -> frequency = pulseIn(sensorOut, LOW);
    frequency = pulseIn(sensorOut, digitalRead(sensorOut) == HIGH ? LOW : HIGH);  // Reading the output frequency
    /*some sites uses ->
       frequency = map(frequency, 25,70,0,255);
       === to Remaping the value of the frequency to the RGB Model of 0 to 255
    */
    blue = 10246 / frequency; // calibration using white color
    // blue = 6630 / frequency; old calibration
    blue = blue > 255 ? 255 : blue;
    // Serial.print("B= ");
    //  bluetooth.print(blue);//printing Blue color frequency
    Serial.print(blue);

    //######################################################
    // combining all
    rgb = (String)red + " " + (String)green + " " + (String)blue + " ";
    //sending to bluetooth
    bluetooth.print(rgb);

    Serial.println();
    Serial.print("Bluetooth Data Send: ");
    Serial.print(rgb);
    Serial.println();

    // Rotate CW turn quickly
    StepsRequired  = STEPS_PER_OUT_REV / 4;
    steppermotor.setSpeed(500);
    steppermotor.step(StepsRequired);
    delay(4000);

  }
}
