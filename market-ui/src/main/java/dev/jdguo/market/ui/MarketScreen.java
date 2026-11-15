package dev.jdguo.market.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import java.util.ArrayList;
import java.util.List;

/** Exchange-style view over actual server listings, never a simulated order book. */
public final class MarketScreen extends Screen {
    private JsonObject data;
    private MarketLayout.Layout layout;
    private String selected = "", currency = "", price = "1", count = "1", notice = "", filter = "";
    private int tab, page;
    private boolean mine, confirming, pending;
    private TextFieldWidget priceField, countField, filterField;
    private ButtonWidget buyButton, listButton;
    private final List<JsonObject> rows = new ArrayList<>();
    public MarketScreen(JsonObject data) { super(Text.literal("市场交易终端")); this.data = data; }
    static String text(JsonObject o, String key) {
        try { return o.has(key) && !o.get(key).isJsonNull() ? o.get(key).getAsString() : ""; }
        catch (RuntimeException e) { return ""; }
    }
    private List<JsonObject> currencies() {
        List<JsonObject> result = new ArrayList<>();
        if (data.has("currencies") && data.get("currencies").isJsonArray())
            for (JsonElement value : data.getAsJsonArray("currencies")) if (value.isJsonObject()) result.add(value.getAsJsonObject());
        return result;
    }
    private String currencyLabel() {
        for (JsonObject value : currencies()) if (currency.equals(text(value,"id"))) return text(value,"label");
        return currency.isBlank() ? "等待币种" : currency;
    }
    void update(JsonObject next) { capture(); data = next; confirming = false; rebuild(); }
    void result(JsonObject result) {
        pending = false; confirming = false;
        notice = "";
        if (result.has("lines")) for (JsonElement line : result.getAsJsonArray("lines")) notice += line.getAsString() + " ";
        rebuild();
    }
    private void capture() {
        if (priceField != null) price = priceField.getText();
        if (countField != null) count = countField.getText();
        if (filterField != null) filter = filterField.getText();
    }
    private void rebuild() { if (client != null) { clearChildren(); init(); } }
    @Override protected void init() {
        layout = MarketLayout.at(width, height);
        if (width < 320 || height < 240) { button("关闭", Math.max(4,width-62), 8, 54, this::close); return; }
        priceField = null; countField = null; filterField = null;
        if (currency.isBlank()) currency = text(data, "defaultCurrency");
        if (currency.isBlank()) currency = "note";
        if (!currencies().isEmpty() && currencies().stream().noneMatch(c -> currency.equals(text(c,"id")))) currency = text(currencies().get(0),"id");
        button("刷新", width-116, 8, 50, () -> { capture(); confirming=false; if (!MarketUiClient.refresh()) notice="服务器未提供市场接口，请安装服务端 marketmod。"; });
        button("关闭", width-62, 8, 54, this::close);
        button(mine ? "我的挂单" : "全部挂牌", 8, 34, 82, () -> { capture(); mine=!mine; page=0; rebuild(); });
        if (layout.compact()) {
            int w = Math.max(44, (width-106)/3);
            for (int i=0;i<3;i++) { int target=i; button(new String[]{"行情","详情","下单"}[i], 96+i*w,34,w-4,()-> {capture(); tab=target; rebuild();}); }
        }
        rows.clear();
        if (data.has("listings")) for (JsonElement element:data.getAsJsonArray("listings")) {
            JsonObject row=element.getAsJsonObject();
            boolean own=client.player!=null && client.player.getUuidAsString().equals(text(row,"sellerId"));
            if ((!mine||own) && (filter.isBlank() || (name(row)+text(row,"sellerName")+text(row,"currency")).toLowerCase().contains(filter.toLowerCase()))) rows.add(row);
        }
        if (!layout.compact()||tab==0) listingPane();
        if (!layout.compact()||tab==1) detailsPane();
        if (!layout.compact()||tab==2) orderPane();
    }
    private void listingPane() {
        var b=layout.listings();
        filterField = field(b.x()+6,b.y()+22,b.width()-12,filter,"搜索物品/卖方/币种",64);
        filterField.setChangedListener(value -> { filter=value; });
        int capacity=Math.max(1,(b.height()-88)/34); int max=Math.max(0,(rows.size()-1)/capacity); page=Math.min(page,max);
        for(int i=0;i<capacity && page*capacity+i<rows.size();i++) {
            JsonObject row=rows.get(page*capacity+i); int y=b.y()+48+i*34;
            button(fit(name(row)+" ×"+text(row,"count"),b.width()-26), b.x()+6,y,b.width()-12,()->{capture();selected=text(row,"id");confirming=false;if(layout.compact())tab=1;rebuild();});
        }
        button("◀",b.x()+6,b.y()+b.height()-24,32,()->{capture();page=Math.max(0,page-1);rebuild();});
        button("搜索",b.x()+42,b.y()+b.height()-24,48,()->{capture();page=0;rebuild();});
        button("▶",b.x()+b.width()-38,b.y()+b.height()-24,32,()->{capture();page++;rebuild();});
    }
    private JsonObject selection() {
        if(data.has("listings")) for(JsonElement element:data.getAsJsonArray("listings")) if(selected.equals(text(element.getAsJsonObject(),"id")))return element.getAsJsonObject();
        return null;
    }
    private void detailsPane() {
        var b=layout.details(); JsonObject row=selection(); if(row==null)return;
        boolean own=client.player!=null && client.player.getUuidAsString().equals(text(row,"sellerId"));
        int y=b.y()+b.height()-25;
        buyButton=button(own ? "撤销我的挂牌" : confirming ? "确认整笔买入" : "买入整笔",b.x()+6,y,b.width()-12,()->{
            if(pending)return;
            if(!own&&!confirming){confirming=true;notice="请核对币种与总价，再次点击确认。";rebuild();return;}
            send(own ? "cancel "+text(row,"id") : "buy "+text(row,"id")+" "+text(row,"totalPrice"));
        });
        buyButton.active=!pending && (own || (Boolean.parseBoolean(text(data,"maximumTotal")) && text(row,"totalPrice").matches("[0-9]{1,64}")));
    }
    private void orderPane() {
        var b=layout.order(); int x=b.x()+6,w=b.width()-12,y=b.y()+25;
        button(fit("结算币种："+currencyLabel(),w-8),x,y,w,()->{
            capture(); List<JsonObject> cs=currencies(); if(cs.isEmpty()){notice="当前服务器未提供币种选择，请更新市场模组。";return;}
            int i=0;for(;i<cs.size();i++)if(currency.equals(text(cs.get(i),"id")))break;
            currency=text(cs.get((i+1)%cs.size()),"id");rebuild();
        });
        priceField=field(x,y+42,(w-6)/2,price,"每件价格",18);
        countField=field(x+(w+6)/2,y+42,(w-6)/2,count,"数量",4);
        listButton=button("挂牌手中物品",x,y+68,w,()->{
            capture(); try {
                long p=Long.parseLong(price);int n=Integer.parseInt(count);
                if(p<=0||n<=0||n>64)throw new IllegalArgumentException();
                if(currencies().isEmpty()){notice="服务器尚未支持指定币种挂牌。";return;}
                send("list "+p+" "+n+" "+currency);
            }catch(IllegalArgumentException e){notice="价格须为正整数，数量 1–64，且不能超过手持物品。";}
        });
        listButton.active=!pending && b.height()>=112;
    }
    private void send(String action) {
        if(MarketUiClient.action(action)){pending=true;confirming=false;notice="已提交，等待服务器结果；请勿重复操作。";rebuild();}
        else notice="未连接市场服务，操作未发送。";
    }
    private ButtonWidget button(String label,int x,int y,int w,Runnable action){return addDrawableChild(ButtonWidget.builder(Text.literal(label),b->action.run()).dimensions(x,y,Math.max(1,w),18).build());}
    private TextFieldWidget field(int x,int y,int w,String value,String label,int length){var f=new TextFieldWidget(textRenderer,x,y,w,18,Text.literal(label));f.setMaxLength(length);f.setText(value);addDrawableChild(f);return f;}
    private String fit(String value,int pixels){return textRenderer.trimToWidth(value,Math.max(1,pixels));}
    private String name(JsonObject row){String value=text(row,"itemId");return value.isBlank()?"领地 "+text(row,"parcelName"):value;}
    private void line(DrawContext c,String value,int x,int y,int w,int color){c.drawText(textRenderer,fit(value,w),x,y,color,false);}
    private void panel(DrawContext c,MarketLayout.Box b,String title){c.fill(b.x(),b.y(),b.x()+b.width(),b.y()+b.height(),0xff152130);line(c,title,b.x()+6,b.y()+7,b.width()-12,0xffdde8f2);}
    @Override public void render(DrawContext c,int mx,int my,float delta){
        renderBackground(c);c.fill(0,0,width,height,0xff0b1420);
        if(width<320 || height<240) {
            line(c,"请降低 GUI 缩放，界面至少需要 320×240 像素。",8,42,width-16,0xffffc66d);
            super.render(c,mx,my,delta); return;
        }
        line(c,"MARKET  /  市场交易终端",8,12,width-132,0xff6ce0bb);
        if(width<320||height<220){line(c,"建议降低 GUI 缩放，至少需要 320×220 界面像素。",8,height-36,width-16,0xffffc66d);}
        if(!layout.compact()||tab==0){var b=layout.listings();panel(c,b,mine?"我的委托 / 挂牌":"卖出挂牌 / 行情列表");int capacity=Math.max(1,(b.height()-88)/34);for(int i=0;i<capacity&&page*capacity+i<rows.size();i++){JsonObject row=rows.get(page*capacity+i);line(c,text(row,"totalPrice")+" "+text(row,"currency")+" · "+text(row,"sellerName"),b.x()+8,b.y()+69+i*34,b.width()-16,0xff99abbc);}if(rows.isEmpty())line(c,"暂无符合条件的挂牌",b.x()+8,b.y()+55,b.width()-16,0xff99abbc);}
        if(!layout.compact()||tab==1){var b=layout.details();panel(c,b,"交易详情 / 真实挂牌");JsonObject row=selection();int y=b.y()+28;String[] lines=row==null?new String[]{"选择左侧挂牌查看详情","尚无 K 线与买盘历史数据"}:new String[]{name(row),"卖方："+text(row,"sellerName"),"单价："+text(row,"unitPrice")+" "+text(row,"currency"),"总额："+text(row,"totalPrice")+" "+text(row,"currency"),"数量："+text(row,"count"),"整笔成交，不支持部分买入","资金由银行按挂牌币种结算",Boolean.parseBoolean(text(row,"shopPrice"))?"浮动价格：成交时重读商店报价":"固定挂牌价","K 线 / 买盘：尚无数据接口"};for(String l:lines){if(y>b.y()+b.height()-42)break;line(c,l,b.x()+8,y,b.width()-16,0xffb5c5d5);y+=14;}}
        if(!layout.compact()||tab==2){var b=layout.order();panel(c,b,"卖出 / 新挂牌");line(c,"每件价（整数）",b.x()+6,b.y()+55,(b.width()-18)/2,0xff99abbc);line(c,"数量",b.x()+6+(b.width()-6)/2,b.y()+55,(b.width()-18)/2,0xff99abbc);}
        c.enableScissor(4,height-44,width-4,height-4);line(c,notice.isBlank()?(Boolean.parseBoolean(text(data,"truncated")) ? "当前仅显示部分挂牌，请用 /market search 查询" : "无自动换汇 · 个人币交易使用 /market note"):notice,8,height-38,width-16,0xffffc66d);c.disableScissor();
        super.render(c,mx,my,delta);
        if ((!layout.compact() || tab == 1) && selection() != null) {
            var b=layout.details();
            if(mx>=b.x() && mx<b.x()+b.width() && my>=b.y()+24 && my<b.y()+b.height()-30) {
                var row=selection();
                c.drawTooltip(textRenderer, textRenderer.wrapLines(Text.literal(name(row)+"\n卖方："+text(row,"sellerName")+"\n单价："+text(row,"unitPrice")+" "+text(row,"currency")+"\n总额："+text(row,"totalPrice")+" "+text(row,"currency")+"\n完整编号："+text(row,"id")), Math.max(120,width-48)), mx, my);
            }
        }
    }
    @Override public void resize(net.minecraft.client.MinecraftClient client, int width, int height) { capture(); super.resize(client,width,height); }
    @Override public boolean shouldPause(){return false;}
    @Override public void removed(){capture();}
}
