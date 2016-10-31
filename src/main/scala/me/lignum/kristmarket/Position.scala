package me.lignum.kristmarket

class Position(val x: Int = 0, val y: Int = 0, val z: Int = 0) {
  def compare(pos: Position) = x == pos.x && y == pos.y && z == pos.z

  def compare(_x: Int, _y: Int, _z: Int) = _x == x && _y == y && _z == z
}
