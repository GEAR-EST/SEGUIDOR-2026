TaskHandle_t Task1;
TaskHandle_t Task2;
const int LED_PIN = 2;
const int OTHER_LED = 4; 

void setup() {
  Serial.begin(115200);
  pinMode(LED_PIN, OUTPUT);
  pinMode (OTHER_LED, OUTPUT);

  xTaskCreatePinnedToCore(
    Task1Code,
    "Tarefa 1",
    2000,
    NULL,
    2,
    &Task1,
    0
  );

  xTaskCreatePinnedToCore(
    Task2Code,
    "Tarefa 2",
    2000,
    NULL,
    2,
    &Task2,
    1
  );



}

void loop() {

}

//faz o blink no led vermelho (core 0)
void Task1Code(void*pvParameters)
{
  for (;;)
  {
    digitalWrite(LED_PIN, HIGH);
    delay(500);
    digitalWrite(LED_PIN, LOW);
    delay(500);
    Serial.print("Porta do led vermelho: ");
    Serial.println(xPortGetCoreID());
    delay(500);
  }
}

// Faz o blink no led amarelo (core 1)
void Task2Code (void * pvParameters)
{
  for (;;)
  {
    digitalWrite(OTHER_LED, HIGH);
    delay(1000);
    digitalWrite(OTHER_LED, LOW);
    delay(1000);
    Serial.print("Porta do led amarelo: ");
    Serial.println(xPortGetCoreID());
    delay(1000);
  }
}
