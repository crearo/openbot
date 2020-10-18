#define PIN_PWM_L1 5
#define PIN_PWM_L2 6
#define PIN_PWM_R1 9
#define PIN_PWM_R2 10
#define PIN_SPEED_L 2
#define PIN_SPEED_R 3

int ctrl_left = 0;
int ctrl_right = 0;

bool ctrl_rx = false;
String inString = "";

// speed

const unsigned long SPEED_TRIGGER_THRESHOLD = 1; // Triggers within this time will be ignored (ms)
const unsigned int DISK_HOLES = 20;
unsigned long oldtime_left = 0;
unsigned long curtime_left = 0;
unsigned long oldtime_right = 0;
unsigned long curtime_right = 0;
int counter_left = 0;
int counter_right = 0;

unsigned long send_timeout = 0;

void setup() {
  pinMode(PIN_PWM_L1, OUTPUT);
  pinMode(PIN_PWM_L2, OUTPUT);
  pinMode(PIN_PWM_R1, OUTPUT);
  pinMode(PIN_PWM_R2, OUTPUT);

  pinMode(PIN_SPEED_L,INPUT);
  pinMode(PIN_SPEED_R,INPUT);

  Serial.begin(115200, SERIAL_8N1); // 8 data bits, no parity, 1 stop bit

  attachInterrupt(digitalPinToInterrupt(PIN_SPEED_L), update_speed_left, CHANGE);
  attachInterrupt(digitalPinToInterrupt(PIN_SPEED_R), update_speed_right, CHANGE);
  
  send_timeout = millis() + 1000;
}

void loop() {
  if (Serial.available() > 0) {
    read_msg();
  }

  // motors don't move between (0, 60)
  if (ctrl_left > 0 && ctrl_left < 60) {
    ctrl_left = 0;
  }
  if (ctrl_right > 0 && ctrl_right < 60) {
    ctrl_right = 0;
  }
  
  update_left_motors();
  update_right_motors();

  if (millis() >= send_timeout) {
    Serial.print(counter_left);
    Serial.print(", ");
    Serial.println(counter_right);

    send_timeout = millis() + 1000;
    counter_right = 0;
    counter_left = 0;
  }
}

void read_msg() {
  ctrl_rx = true;
  while (ctrl_rx) {
    if (Serial.available()) {
      char inChar = Serial.read();
      // comma indicates that inString contains the left ctrl
      if (inChar == ',') {
        ctrl_left = inString.toInt();
        // clear the string for new input:
        inString = "";
      }
      // new line indicates that inString contains the right ctrl
      else if (inChar == '\n') {
        ctrl_right = inString.toInt();
        // clear the string for new input:
        inString = "";
        // end of message
        ctrl_rx = false;
      }
      else {
        // As long as the incoming byte
        // is not a newline or comma,
        // convert the incoming byte to a char
        // and add it to the string
        inString += inChar;
      }
    }
  }
}

void update_left_motors() {
    if (ctrl_left < 0) {
      analogWrite(PIN_PWM_L1, -ctrl_left);
      analogWrite(PIN_PWM_L2, 0);
    }
    else if (ctrl_left > 0) {
      analogWrite(PIN_PWM_L1, 0);
      analogWrite(PIN_PWM_L2, ctrl_left);
    }
    else { //Motor brake
      analogWrite(PIN_PWM_L1, 255);
      analogWrite(PIN_PWM_L2, 255);
    }
}

void update_right_motors() {
    if (ctrl_right < 0) {
      analogWrite(PIN_PWM_R1, -ctrl_right);
      analogWrite(PIN_PWM_R2, 0);
    }
    else if (ctrl_right > 0) {
      analogWrite(PIN_PWM_R1, 0);
      analogWrite(PIN_PWM_R2, ctrl_right);
    }
    else { //Motor brake
      analogWrite(PIN_PWM_R1, 255);
      analogWrite(PIN_PWM_R2, 255);
    }
}

void update_speed_left() {
  curtime_left = millis();
  if( (curtime_left - oldtime_left) > SPEED_TRIGGER_THRESHOLD ) {
    if (ctrl_left < 0) {
      counter_left--; 
    }
    else if (ctrl_left > 0) {
      counter_left++;
    }
    oldtime_left = curtime_left;
  }
}

void update_speed_right() {
  curtime_right = millis();
  if( (curtime_right - oldtime_right) > SPEED_TRIGGER_THRESHOLD ) {
    if (ctrl_right < 0) {
      counter_right--; 
    }
    else if (ctrl_right > 0){
      counter_right++;
    }
    oldtime_right = curtime_right;
  }
}
