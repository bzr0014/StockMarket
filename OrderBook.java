package pkg.order;
import java.util.ArrayList;
import java.util.HashMap;
import pkg.exception.StockMarketExpection;
import pkg.market.Market;
import pkg.market.api.*;
public class OrderBook {
	Market market;
	HashMap<String, ArrayList<Order>> buyOrders;
	HashMap<String, ArrayList<Order>> sellOrders;
	PriceSetter priceSetter;
	public OrderBook(Market market) {
		this.market = market;
		buyOrders = new HashMap<String, ArrayList<Order>>();
		sellOrders = new HashMap<String, ArrayList<Order>>();
		priceSetter = new PriceSetter();
	}

	public void addToOrderBook(Order order) {
		if (BuyOrder.class.isInstance(order)){
			if (this.buyOrders.containsKey(order.getStockSymbol())){
				ArrayList<Order> buyOrderList =buyOrders.get(order.getStockSymbol());
				buyOrderList.add(order);
			}
			else{
				ArrayList<Order> buyOrderList = new ArrayList<Order>();
				buyOrderList.add(order);
				this.buyOrders.put(order.getStockSymbol(), buyOrderList);
			}
		}
		else{
			if (this.sellOrders.containsKey(order.getStockSymbol())){
				ArrayList<Order> sellOrderList =sellOrders.get(order.getStockSymbol());
				sellOrderList.add(order);
			}
			else{
				ArrayList<Order> sellOrderList = new ArrayList<Order>();
				sellOrderList.add(order);
				this.sellOrders.put(order.getStockSymbol(), sellOrderList);
			}
		}
	}

	public void trade() {
		for (String stockSymbol : this.sellOrders.keySet()){
			if (this.buyOrders.containsKey(stockSymbol)){
				double matchPrice  = matchingPrice(this.sellOrders.get(stockSymbol), this.buyOrders.get(stockSymbol));
				if (matchPrice>0){
					try{
						market.updateStockPrice(stockSymbol, matchPrice);
					}
					catch (StockMarketExpection e){};
					int stockTradeSize =0;

					for (Order sellOrder : this.sellOrders.get(stockSymbol)){
						if (sellOrder.getPrice()<=matchPrice){
							try{
								sellOrder.getTrader().tradePerformed(sellOrder, matchPrice);
								stockTradeSize += sellOrder.getSize();
							}
							catch (StockMarketExpection e){};
							this.sellOrders.remove(sellOrder);
						}
					}
					//updating the order of stock in buy order
					ArrayList<Order> tempOrders  = sortOrderPrice(this.buyOrders.get(stockSymbol));
					this.buyOrders.remove(stockSymbol);
					this.buyOrders.put(stockSymbol,tempOrders);

					for (Order buyOrder : this.buyOrders.get(stockSymbol)){
						if (buyOrder.getPrice()>=matchPrice && stockTradeSize>0){
							try{
								if (stockTradeSize>buyOrder.getSize())
								{
									buyOrder.getTrader().tradePerformed(buyOrder, matchPrice);
									stockTradeSize -= buyOrder.getSize();
								}
								else{
									buyOrder.setSize(stockTradeSize);
									buyOrder.getTrader().tradePerformed(buyOrder, matchPrice);
									stockTradeSize = 0;
								}

							}
							catch (StockMarketExpection e){};
							this.buyOrders.remove(buyOrder);
						}
					}
				}
			}
		}
	}
	private double matchingPrice(ArrayList<Order> sellOrderList, ArrayList<Order> buyOrderList){
		ArrayList<double[]> sellPriceList = new  ArrayList<double[]>();
		ArrayList<double[]> buyPriceList = new ArrayList<double[]>();
		updatePriceList(sellOrderList, sellPriceList);
		updatePriceList(buyOrderList, buyPriceList);

		double matchPrice =0;
		for (int i = sellPriceList.size()-1; i>=0; i--){
			double bidPrice = sellPriceList.get(i)[0];
			double sellAmount = sellPriceList.get(i)[2];
			double buyAmount = 0;
			for (int j = 0;j <buyPriceList.size();j++ ){
				if (buyPriceList.get(j)[0]>=bidPrice){
					buyAmount = buyPriceList.get(j)[2];
					break;
				}
			}
			if (sellAmount<=buyAmount){
				matchPrice = bidPrice;
				break;
			}
		}
		return matchPrice;
	}

	private void updatePriceList(ArrayList<Order> orderList,
			ArrayList<double[]> sellPriceList) {
		for (int i = 0; i<orderList.size(); i++){
			double[] priceSet = new double[3];
			priceSet[0] = orderList.get(i).getPrice();
			priceSet[1] = orderList.get(i).getSize();
			boolean flag = true;
			for (int j =0; j<sellPriceList.size();j++){
				if (priceSet[0]<sellPriceList.get(j)[0]){
					sellPriceList.add(j,priceSet);
					flag = false;
					break;
				}
			}
			if (flag){
				sellPriceList.add(priceSet);
			}
		}

		for(int i = 0; i<sellPriceList.size(); i++){
			if(i>0){
				sellPriceList.get(i)[2] =sellPriceList.get(i-1)[2] + sellPriceList.get(i)[1];
			}
			else{
				sellPriceList.get(0)[2] = sellPriceList.get(0)[1];
			}
		}
	}
	private ArrayList<Order> sortOrderPrice(ArrayList<Order> orders){
		ArrayList<Order> sortedOrders = new ArrayList<Order>();
		boolean flag = true;
		sortedOrders.add(orders.get(0));
		for (int i =1; i<orders.size(); i++){
			for (int j=0; j<sortedOrders.size();j++){
				if (orders.get(i).getPrice()>sortedOrders.get(j).getPrice()){
					sortedOrders.add(j,orders.get(i));
					flag = false;
					break;
				}
			}
			if (flag){
				sortedOrders.add(orders.get(i));
			}
		}
		return sortedOrders;
	}

}
