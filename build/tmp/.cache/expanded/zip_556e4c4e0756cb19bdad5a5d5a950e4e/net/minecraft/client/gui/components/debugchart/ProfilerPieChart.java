package net.minecraft.client.gui.components.debugchart;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfileResults;
import net.minecraft.util.profiling.ResultField;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ProfilerPieChart {
    private static final int RADIUS = 105;
    private static final int MARGIN = 5;
    private static final int CHART_Z_OFFSET = 10;
    private final Font font;
    @Nullable
    private ProfileResults profilerPieChartResults;
    private String profilerTreePath = "root";
    private int bottomOffset = 0;

    public ProfilerPieChart(Font pFont) {
        this.font = pFont;
    }

    public void setPieChartResults(@Nullable ProfileResults pPieChartResults) {
        this.profilerPieChartResults = pPieChartResults;
    }

    public void setBottomOffset(int pBottomOffset) {
        this.bottomOffset = pBottomOffset;
    }

    public void render(GuiGraphics pGuiGraphics) {
        if (this.profilerPieChartResults != null) {
            List<ResultField> list = this.profilerPieChartResults.getTimes(this.profilerTreePath);
            ResultField resultfield = list.removeFirst();
            int i = pGuiGraphics.guiWidth() - 105 - 10;
            int j = i - 105;
            int k = i + 105;
            int l = list.size() * 9;
            int i1 = pGuiGraphics.guiHeight() - this.bottomOffset - 5;
            int j1 = i1 - l;
            int k1 = 62;
            int l1 = j1 - 62 - 5;
            pGuiGraphics.fill(j - 5, l1 - 62 - 5, k + 5, i1 + 5, -1873784752);
            pGuiGraphics.drawSpecial(p_366943_ -> {
                double d0 = 0.0;

                for (ResultField resultfield2 : list) {
                    int i3 = Mth.floor(resultfield2.percentage / 4.0) + 1;
                    VertexConsumer vertexconsumer = p_366943_.getBuffer(RenderType.debugTriangleFan());
                    int j3 = ARGB.opaque(resultfield2.getColor());
                    int k3 = ARGB.multiply(j3, -8355712);
                    PoseStack.Pose posestack$pose = pGuiGraphics.pose().last();
                    vertexconsumer.addVertex(posestack$pose, i, l1, 10.0F).setColor(j3);

                    for (int l3 = i3; l3 >= 0; l3--) {
                        float f = (float)((d0 + resultfield2.percentage * l3 / i3) * (float) (Math.PI * 2) / 100.0);
                        float f1 = Mth.sin(f) * 105.0F;
                        float f2 = Mth.cos(f) * 105.0F * 0.5F;
                        vertexconsumer.addVertex(posestack$pose, i + f1, l1 - f2, 10.0F).setColor(j3);
                    }

                    vertexconsumer = p_366943_.getBuffer(RenderType.debugQuads());

                    for (int i4 = i3; i4 > 0; i4--) {
                        float f6 = (float)((d0 + resultfield2.percentage * i4 / i3) * (float) (Math.PI * 2) / 100.0);
                        float f7 = Mth.sin(f6) * 105.0F;
                        float f8 = Mth.cos(f6) * 105.0F * 0.5F;
                        float f3 = (float)((d0 + resultfield2.percentage * (i4 - 1) / i3) * (float) (Math.PI * 2) / 100.0);
                        float f4 = Mth.sin(f3) * 105.0F;
                        float f5 = Mth.cos(f3) * 105.0F * 0.5F;
                        if (!((f8 + f5) / 2.0F > 0.0F)) {
                            vertexconsumer.addVertex(posestack$pose, i + f7, l1 - f8, 10.0F).setColor(k3);
                            vertexconsumer.addVertex(posestack$pose, i + f7, l1 - f8 + 10.0F, 10.0F).setColor(k3);
                            vertexconsumer.addVertex(posestack$pose, i + f4, l1 - f5 + 10.0F, 10.0F).setColor(k3);
                            vertexconsumer.addVertex(posestack$pose, i + f4, l1 - f5, 10.0F).setColor(k3);
                        }
                    }

                    d0 += resultfield2.percentage;
                }
            });
            DecimalFormat decimalformat = new DecimalFormat("##0.00");
            decimalformat.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ROOT));
            String s = ProfileResults.demanglePath(resultfield.name);
            String s1 = "";
            if (!"unspecified".equals(s)) {
                s1 = s1 + "[0] ";
            }

            if (s.isEmpty()) {
                s1 = s1 + "ROOT ";
            } else {
                s1 = s1 + s + " ";
            }

            int i2 = 16777215;
            int j2 = l1 - 62;
            pGuiGraphics.drawString(this.font, s1, j, j2, 16777215);
            s1 = decimalformat.format(resultfield.globalPercentage) + "%";
            pGuiGraphics.drawString(this.font, s1, k - this.font.width(s1), j2, 16777215);

            for (int k2 = 0; k2 < list.size(); k2++) {
                ResultField resultfield1 = list.get(k2);
                StringBuilder stringbuilder = new StringBuilder();
                if ("unspecified".equals(resultfield1.name)) {
                    stringbuilder.append("[?] ");
                } else {
                    stringbuilder.append("[").append(k2 + 1).append("] ");
                }

                String s2 = stringbuilder.append(resultfield1.name).toString();
                int l2 = j1 + k2 * 9;
                pGuiGraphics.drawString(this.font, s2, j, l2, resultfield1.getColor());
                s2 = decimalformat.format(resultfield1.percentage) + "%";
                pGuiGraphics.drawString(this.font, s2, k - 50 - this.font.width(s2), l2, resultfield1.getColor());
                s2 = decimalformat.format(resultfield1.globalPercentage) + "%";
                pGuiGraphics.drawString(this.font, s2, k - this.font.width(s2), l2, resultfield1.getColor());
            }
        }
    }

    public void profilerPieChartKeyPress(int pIndex) {
        if (this.profilerPieChartResults != null) {
            List<ResultField> list = this.profilerPieChartResults.getTimes(this.profilerTreePath);
            if (!list.isEmpty()) {
                ResultField resultfield = list.remove(0);
                if (pIndex == 0) {
                    if (!resultfield.name.isEmpty()) {
                        int i = this.profilerTreePath.lastIndexOf(30);
                        if (i >= 0) {
                            this.profilerTreePath = this.profilerTreePath.substring(0, i);
                        }
                    }
                } else {
                    pIndex--;
                    if (pIndex < list.size() && !"unspecified".equals(list.get(pIndex).name)) {
                        if (!this.profilerTreePath.isEmpty()) {
                            this.profilerTreePath = this.profilerTreePath + "\u001e";
                        }

                        this.profilerTreePath = this.profilerTreePath + list.get(pIndex).name;
                    }
                }
            }
        }
    }
}