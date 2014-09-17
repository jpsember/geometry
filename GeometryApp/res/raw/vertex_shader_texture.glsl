uniform mat4 u_Matrix;  
uniform vec2 u_SpritePosition;

attribute vec4 a_Position;      
attribute vec2 a_TexCoordinate; 
 
varying vec2 v_TexCoordinate;   
 
void main()
{
	v_TexCoordinate = a_TexCoordinate;
	gl_Position = u_Matrix * (a_Position + vec4(u_SpritePosition,0,0));
}
