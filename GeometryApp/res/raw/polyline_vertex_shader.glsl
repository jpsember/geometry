uniform mat4 u_Matrix;
uniform vec4 u_InputColor;
uniform vec2 u_Translation;

attribute vec4 a_Position;

varying vec4 v_Color;

void main() 
{
	v_Color = u_InputColor;

	gl_Position = u_Matrix * (a_Position + vec4(u_Translation,0,0));
}
