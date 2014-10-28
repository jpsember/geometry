package com.js.geometry;

import java.util.Collection;

import com.js.geometryapp.Algorithm;


/**
 * An AlgorithmStepper implementation that does nothing
 */
public class DefaultStepper implements AlgorithmStepper {

	public DefaultStepper() {
	}

	@Override
	public void addAlgorithm(Algorithm delegate) {
	}

	private static final Rect sAlgorithmRect = new Rect(0, 0, 1200, 1000);

	@Override
	public Rect algorithmRect() {
		return sAlgorithmRect;
	}

	@Override
	public boolean isActive() {
		return false;
	}

	@Override
	public void pushActive(boolean active) {
	}

	@Override
	public void popActive() {
	}

	@Override
	public void pushActive(String widgetId) {
	}

	@Override
	public boolean step() {
		return false;
	}

	@Override
	public boolean bigStep() {
		return false;
	}

	@Override
	public void show(String message) {
	}

	@Override
	public void setDoneMessage(String message) {
	}

	@Override
	public boolean openLayer(String key) {
		return false;
	}

	@Override
	public void closeLayer() {
	}

	@Override
	public void removeLayer(String key) {
	}

	@Override
	public String plot(Renderable element) {
		return "";
	}

	@Override
	public String highlight(Renderable element) {
		return "";
	}

	@Override
	public String plot(Point point) {
		return "";
	}

	@Override
	public String highlight(Point point) {
		return "";
	}

	@Override
	public String plot(Point point, float radius) {
		// TODO Auto-generated method stub
		return "";
	}

	@Override
	public String highlight(Point point, float radius) {
		return "";
	}

	@Override
	public String plotRay(Point p1, Point p2) {
		return "";
	}

	@Override
	public String plot(Edge edge) {
		return "";
	}

	@Override
	public String highlightRay(Point p1, Point p2) {
		return "";
	}

	@Override
	public String highlight(Edge edge) {
		return "";
	}

	@Override
	public String plotLine(Point p1, Point p2) {
		return "";
	}

	@Override
	public String highlightLine(Point p1, Point p2) {
		return "";
	}

	@Override
	public String plot(Polygon polygon) {
		return "";
	}

	@Override
	public String plot(Polygon polygon, boolean filled) {
		return "";
	}

	@Override
	public String highlight(Polygon polygon, boolean filled) {
		return "";
	}

	@Override
	public String plotPolyline(Collection<Point> endpoints) {
		return "";
	}

	@Override
	public String highlightPolyline(Collection<Point> endpoints) {
		return "";
	}

	@Override
	public String plot(String text, Point location) {
		return "";
	}

	@Override
	public String highlight(String text, Point location) {
		return "";
	}

	@Override
	public String plotSprite(int spriteResourceId, Point location) {
		return "";
	}

	@Override
	public String highlightSprite(int spriteResourceId, Point location) {
		return "";
	}

	@Override
	public String plotMesh(Mesh meshContext) {
		return "";
	}

	@Override
	public String setColor(int color) {
		return "";
	}

	@Override
	public String setLineWidth(float lineWidth) {
		return "";
	}

	@Override
	public String setNormal() {
		return "";
	}

}
