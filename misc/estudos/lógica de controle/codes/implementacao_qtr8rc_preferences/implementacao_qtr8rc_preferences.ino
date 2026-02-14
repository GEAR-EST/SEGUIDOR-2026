#include <QTRSensors.h>
#include <Preferences.h>

#define RightSensor 9
#define LeftSensor 8

#define EMPTY_VALUE 65535

uint16_t readRight;
uint16_t readLeft;

QTRSensors qtr;
const uint8_t SensorCount = 8;
uint16_t sensorValues[SensorCount];

Preferences preferences

void doCalibration(){
   //entrando em modo calibração
  delay(500);
  digitalWrite(LED_BUILTIN, HIGH);

  //calibrando
  for (uint16_t i=0; i < 400; i++) qtr.calibrate();

  //para depuração, os valores máximo e mínimos na calibração
  uint16_t max_values[8];
  uint16_t min_values[8];

  for (uint8_t i=0; i < 8; i++){
    max_values[i] = EMPTY_VALUE;
    min_values[i] = EMPTY_VALUE;
  }

  Serial.println("Maximum values");
  for(uint16_t i=0; i < SensorCount; i++){
    Serial.print(qtr.calibrationOn.maximum[i]);
    Serial.print(' ');
    max_values[i] = qtr.calibrationOn.maximum[i];
  }

  Serial.println("Minimum values");
  for(uint16_t i=0; i < SensorCount; i++){
    Serial.print(qtr.calibrationOn.minimum[i]);
    Serial.print(' ');
    min_values[i] = qtr.calibration.minimum[i];
  }

  size_t max_values_bytes = sizeof(max_values);
  size_t min_values_bytes = sizeof(min_values);

  preferences.putBytes("max_values_array", max_values, max_values_bytes);
  preferences.putBytes("min_values_array", min_values, min_values_bytes);

}

bool readCalibration(){
  uint16_t read_max[8];
  uint16_t read_min[8];

  size_t read_size_max = preferences.getBytesLength("max_values_array");
  size_t read_size_min = preferences.getBytesLength("min_values_array");
  
  if(read_size_max = sizeof(read_max)) preferences.getBytes("max_values_array", read_max, sizeof(read_max));
  if(read_size_min = sizeof(read_min)) preferences.getBytes("min_values_array", read_min, sizeof(read_min));

  for (auto n : read_max){
    if (n == EMPTY_VALUE) return false;
  }

  return true;
  
}

void setup() {

  Serial.begin(9600);

  //configuração do QTR-8RC
  qtr.setTypeRC(); //tipo de sensor é RC pq é o QTR-8RC duuuuh
  qtr.setSensorPins((const uint8_t[]){3, 4, 5, 6, 7, 8, 9, 10}, SensorCount);
  qtr.setEmitterPin(2);

  preferences.begin("calibration_values", false);

  pinMode(LED_BUILTIN, OUTPUT);

  //verificar se já há valores calibrados
  if (readCalibration() == false) doCalibration();

  //configuração dos sensores laterais
  pinMode(RightSensor, INPUT);
  pinMode(LeftSensor, INPUT);

  preferences.end();

}

void loop() {
  uint16_t pos = qtr.readLineBlack(sensorValues);

  //0 = máxima reflectância e 1000 = mínima reflectância
  for (uint8_t i = 0; i < SensorCount; i++){
    Serial.print(sensorValues[i]);
    Serial.print('\t');
  }
  Serial.println(pos);

  //Sensores laterais
  readRight = digitalRead(RightSensor);
  readLeft = digitalRead(LeftSensor);
  Serial.println("Direito: "+readRight);
  Serial.println("Esquerdo: "+readLeft);

  delay(250);
}