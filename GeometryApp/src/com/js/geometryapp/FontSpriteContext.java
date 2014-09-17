package com.js.geometryapp;

//import static android.opengl.GLES20.GL_FLOAT;
//import static android.opengl.GLES20.GL_LINK_STATUS;
//import static android.opengl.GLES20.GL_TRIANGLES;
//import static android.opengl.GLES20.GL_VALIDATE_STATUS;
//import static android.opengl.GLES20.*;
//import static com.js.basic.Tools.die;
//import static com.js.basic.Tools.unimp;
//import static com.js.basic.Tools.warning;
//
//import java.nio.FloatBuffer;
//
//import android.content.Context;
//import android.graphics.Color;
//
//import com.js.geometry.Point;
//import com.js.geometry.R;

/**
 * @deprecated
 */
public class FontSpriteContext /* extends SpriteContext */{
	//
	// public FontSpriteContext(String transformName) {
	// super(transformName);
	// warning("using blue instead of white");
	// setColor(Color.BLUE);
	// }
	//
	// public void renderSprite(GLTexture mTexture, FloatBuffer vertexData,
	// Point mPosition) {
	// activateProgram();
	//
	// prepareProjection();
	//
	// // Specify offset
	// glUniform2f(mSpritePositionLocation, mPosition.x, mPosition.y);
	//
	// glUniform4fv(mColorLocation, 4, mTextColor, 0);
	//
	// mTexture.select();
	//
	// vertexData.position(0);
	// int stride = TOTAL_COMPONENTS * Mesh.BYTES_PER_FLOAT;
	//
	// glVertexAttribPointer(mPositionLocation, POSITION_COMPONENT_COUNT,
	// GL_FLOAT, false, stride, vertexData);
	// glEnableVertexAttribArray(mPositionLocation);
	//
	// vertexData.position(POSITION_COMPONENT_COUNT);
	// glVertexAttribPointer(mTextureCoordinateLocation,
	// TEXTURE_COMPONENT_COUNT, GL_FLOAT, false, stride, vertexData);
	// glEnableVertexAttribArray(mTextureCoordinateLocation);
	// glDrawArrays(GL_TRIANGLES, 0, 6);
	// }
	//
	// @Override
	// protected int getFragmentShaderResourceId() {
	// return R.raw.fragment_shader_mask;
	// }
	//
	// public void setColor(int color) {
	// mTextColor[0] = Color.red(color) / 255.0f;
	// mTextColor[1] = Color.green(color) / 255.0f;
	// mTextColor[2] = Color.blue(color) / 255.0f;
	// mTextColor[3] = Color.alpha(color) / 255.0f;
	// }
	//
	// protected void prepareAttributes() {
	// // Must agree with vertex_shader_texture.glsl
	// mPositionLocation = glGetAttribLocation(mProgramObjectId, "a_Position");
	// mSpritePositionLocation = glGetUniformLocation(mProgramObjectId,
	// "u_SpritePosition");
	// mTextureCoordinateLocation = glGetAttribLocation(mProgramObjectId,
	// "a_TexCoordinate");
	// mMatrixLocation = glGetUniformLocation(mProgramObjectId, "u_Matrix");
	//
	// // This must agree with fragment_shader_texture.glsl
	// mColorLocation = glGetUniformLocation(mProgramObjectId, "u_Color");
	// }
	//
	//
	//
	// private int mProgramObjectId;
	// private int mPositionLocation;
	// private int mTextureCoordinateLocation;
	// private int mMatrixLocation;
	// private int mSpritePositionLocation;
	// private int mColorLocation;
	// private float[] mTextColor = new float[4];;
}
