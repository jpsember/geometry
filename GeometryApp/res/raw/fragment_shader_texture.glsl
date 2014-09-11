precision mediump float;        
                                
uniform sampler2D u_Texture;    // The input texture.
 
varying vec2 v_TexCoordinate;   
 
void main()
{
    gl_FragColor = texture2D(u_Texture, v_TexCoordinate);
}
