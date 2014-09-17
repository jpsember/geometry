precision mediump float;        
                                
uniform sampler2D u_Texture;
uniform vec4 u_InputColor;

varying vec2 v_TexCoordinate;   
 
void main()
{
	vec4 texColor = texture2D(u_Texture, v_TexCoordinate);
	
	gl_FragColor = vec4(
		u_InputColor.r * texColor.r, 
		u_InputColor.g * texColor.g, 
		u_InputColor.b * texColor.b, 
		u_InputColor.a * texColor.a
	);
	
}
