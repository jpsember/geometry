uniform mat4 u_Matrix;

uniform vec4 u_InputColor;

attribute vec4 a_Position;

varying vec4 v_Color;

void main() 
{
	v_Color = u_InputColor;

	gl_Position = u_Matrix * a_Position;
	gl_PointSize = 10.0;
}
