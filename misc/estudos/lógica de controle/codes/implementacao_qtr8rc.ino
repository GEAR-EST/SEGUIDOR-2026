#include <QTRSensors.h>
#define RightSensor 9
#define LeftSensor 8

uint16_t readRight;
uint16_t readLeft;

QTRSensors qtr;
const uint8_t SensorCount = 8;
uint16_t sensorValues[SensorCount]

void setup() {
  //configuração do QTR-8RC
  qtr.setTypeRC(); //tipo de sensor é RC pq é o QTR-8RC duuuuh
  qtr.setSensorPins((const uint8_t[]){3, 4, 5, 6, 7, 8, 9, 10}, SensorCount);
  qtr.setEmitterPin(2);
  
  //entrando em modo calibração
  delay(500);
  pinMode(LED_BUILTIN, OUTPUT);
  digitalWrite(LED_BUILTIN, HIGH);

  //calibrando
  for (uint16_t i=0; i < 400; i++) qtr.calibrate();

  //para depuração, os valores máximo e mínimos na calibração

  Serial.begin(9600);

  Serial.println("Maximum values");
  for(uint16_t i=0; i < SensorCount; i++){
    Serial.print(qtr.calibrationOn.maximum[i]);
    Serial.print(' ');
  }

  Serial.println("Minimum values");
  for(uint16_t i=0; i < SensorCount; i++){
    Serial.print(qtr.calibrationOn.minimum[i]);
    Serial.print(' ');
  }

  //configuração dos sensores laterais
  pinMode(RightSensor, INPUT);
  pinMode(LeftSensor, INPUT);

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
