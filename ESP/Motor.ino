void forward(int thrust) {
  analogWrite(mFR_1, thrust);
  analogWrite(mFL_1, thrust);
  analogWrite(mBR_1, thrust);
  analogWrite(mBL_1, thrust);
  analogWrite(mFR_2, 0);
  analogWrite(mFL_2, 0);
  analogWrite(mBR_2, 0);
  analogWrite(mBL_2, 0);
}

void backward(int thrust) {
  analogWrite(mFR_2, thrust);
  analogWrite(mFL_2, thrust);
  analogWrite(mBR_2, thrust);
  analogWrite(mBL_2, thrust);
  analogWrite(mFR_1, 0);
  analogWrite(mFL_1, 0);
  analogWrite(mBR_1, 0);
  analogWrite(mBL_1, 0);
}

void left(int thrust) {
  analogWrite(mFR_1, thrust);
  analogWrite(mFL_1, 0);
  analogWrite(mBR_1, thrust);
  analogWrite(mBL_1, 0);
  analogWrite(mFR_2, 0);
  analogWrite(mFL_2, thrust);
  analogWrite(mBR_2, 0);
  analogWrite(mBL_2, thrust);
}

void right(int thrust) {
  analogWrite(mFR_1, 0);
  analogWrite(mFL_1, thrust);
  analogWrite(mBR_1, 0);
  analogWrite(mBL_1, thrust);
  analogWrite(mFR_2, thrust);
  analogWrite(mFL_2, 0);
  analogWrite(mBR_2, thrust);
  analogWrite(mBL_2, 0);
}
