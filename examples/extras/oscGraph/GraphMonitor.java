import java.util.ArrayList;
import processing.core.PApplet;
import processing.core.PGraphics;

public class GraphMonitor {
  private int COLOR_PATTENS[];

  private PApplet app;
  private String title;
  private int xOffset, yOffset;
  private int xWidth, yHeight;
  private ArrayList<float[]> params;
  private float maxRange;

  public GraphMonitor(PApplet _app) {
    app = _app;

    COLOR_PATTENS =
        new int[] {
          _app.color(255, 0, 0),
          _app.color(0, 255, 0),
          _app.color(0, 0, 255),
          _app.color(255, 255, 0),
          _app.color(255, 0, 255),
          _app.color(0, 255, 255),
        };
  }

  public void setup(String _title, int _xOffset, int _yOffset, int _xWidth, int _yHeight) {
    title = _title;
    xOffset = _xOffset;
    yOffset = _yOffset;
    xWidth = _xWidth;
    yHeight = _yHeight;
    params = new ArrayList<float[]>();
  }

  public void addGraph(float... _values) {
    maxRange = 1;
    for (int valueIndex = 0; valueIndex < _values.length; valueIndex++) {
      if (valueIndex >= params.size()) {
        params.add(new float[xWidth]);
      }
      float[] pValues = params.get(valueIndex);
      pValues[xWidth - 1] = _values[valueIndex];
      for (int j = 0; j < xWidth - 1; j++) {
        pValues[j] = pValues[j + 1];
        maxRange = (app.abs(pValues[j]) > maxRange ? app.abs(pValues[j]) : maxRange);
      }
    }
  }

  public void graphDraw(PGraphics _pg) {
    _pg.pushMatrix();

    _pg.translate(xOffset, yOffset);
    _pg.fill(240);
    _pg.stroke(130);
    _pg.strokeWeight(1);
    _pg.rect(0, 0, xWidth, yHeight);
    _pg.line(0, yHeight / 2, xWidth, yHeight / 2);

    _pg.textSize(25);
    _pg.fill(60);
    _pg.textAlign(app.LEFT, app.BOTTOM);
    _pg.text(title + "(params: " + params.size() + ")", 20, -5);
    _pg.textSize(22);
    _pg.textAlign(app.RIGHT);
    _pg.text(0, -5, yHeight / 2 + 7);
    _pg.text(app.nf(maxRange, 0, 1), -5, 18);
    _pg.text(app.nf(-1 * maxRange, 0, 1), -5, yHeight);

    _pg.translate(0, yHeight / 2);
    _pg.scale(1, -1);
    _pg.strokeWeight(1);
    for (int i = 0; i < xWidth - 1; i++) {
      for (int pi = 0; pi < params.size(); pi++) {
        float[] pValues = params.get(pi);

        _pg.stroke(COLOR_PATTENS[pi % COLOR_PATTENS.length]);
        _pg.line(
            i,
            pValues[i] * (yHeight / 2) / maxRange,
            i + 1,
            pValues[i + 1] * (yHeight / 2) / maxRange);
      }
    }
    _pg.popMatrix();
  }

  public void setOffset(int _xOffset, int _yOffset) {
    xOffset = _xOffset;
    yOffset = _yOffset;
  }
}
