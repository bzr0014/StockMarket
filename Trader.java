package pkg.trader;
import java.util.ArrayList;
import pkg.exception.StockMarketExpection;
import pkg.market.Market;
import pkg.order.BuyOrder;
import pkg.order.Order;
import pkg.order.OrderType;
import pkg.order.SellOrder;

public class Trader {
	String name;
	// Cash left in the trader's hand
	double cashInHand;
	// Stocks owned by the trader
	ArrayList<Order> position;
	// Orders placed by the trader
	ArrayList<Order> ordersPlaced;

	public Trader(String traderName, double cashInHand) {
		super();
		this.name = name;
		this.cashInHand = cashInHand;
		this.position = new ArrayList<Order>();
		this.ordersPlaced = new ArrayList<Order>();
	}

	public void buyFromBank(Market market, String symbol, int volume)
			throws StockMarketExpection {
		double price = market.getStockForSymbol(symbol).getPrice();
		if (cashInHand < price * volume) {
			throw new StockMarketExpection("Stock's price is larger than the cash possessed. Trader: "
					+ this.name);
		}
		BuyOrder order = new BuyOrder(symbol, volume, price, this);
		cashInHand = cashInHand - price * volume;
		position.add(order);

	}

	public void placeNewOrder(Market market, String symbol, int volume,
			double price, OrderType orderType) throws StockMarketExpection {
		if (price < 0) {
			throw new StockMarketExpection("Invalid Price: price is negative");
		}
		if (volume <= 0) {
			throw new StockMarketExpection("Invalid volume: volume is invalid");
		}
		// A trader cannot place two orders for the same stock,
		if (!findOrderInList(symbol, ordersPlaced).isEmpty()) {
			throw new StockMarketExpection("The order for this stock is already placed. trader: "
					+ this.name);
		}
		// Note that no trade has been made yet. The order is in suspension
		// until a trade is triggered.
		Order order;
		if (orderType == OrderType.BUY) {
			if (volume * price > cashInHand) {
				throw new StockMarketExpection("Stock price is larger than the cash possessed. trader: "
						+ this.name);
			}
			order = new BuyOrder(symbol, volume, price, this);
		}
		else {
			//A person cannot place a sell order for a stock that he does not own.
			if (findOrderInList(symbol, position).isEmpty()) {
				throw new StockMarketExpection("The stock is not in position. Trader: "
						+ this.name);
			}
			//he cannot sell more stocks than he possesses.
			int orderSizePosessed = getSizeOfStockFromList(symbol, position);
			if (orderSizePosessed < volume) {
				throw new StockMarketExpection("The order volume is more than possessed. Trader: "
						+ this.name + ". Stocks size posessed: " + orderSizePosessed
						+ ", Amount ordered: " + volume);
			}
			order = new SellOrder(symbol, volume, price, this);
		}
		ordersPlaced.add(order);
		// Also enter the order into the orderbook of the market.
		market.addOrder(order);
	}

	public void placeNewMarketOrder(Market market, String symbol, int volume,
			double price, OrderType orderType) throws StockMarketExpection {
		if (price < 0) {
			throw new StockMarketExpection("Invalid Price: price is negative");
		}

		if (volume <= 0) {
			throw new StockMarketExpection("Invalid volume: volume is invalid");
		}

		if (!findOrderInList(symbol, ordersPlaced).isEmpty()) {
			throw new StockMarketExpection("The order for this stock is already placed. trader: "
					+ this.name);
		}
		Order order;
		if (orderType == OrderType.BUY) {
			if (volume * market.getStockForSymbol(symbol).getPrice() > cashInHand) {
				throw new StockMarketExpection("Stock price is larger than the cash possessed. trader: "
						+ this.name);
			}
			order = new BuyOrder(symbol, volume, true, this);
			order.setPrice(1000000);
		}
		else {
			//A person cannot place a sell order for a stock that he does not own.
			if (findOrderInList(symbol, position).isEmpty()) {
				throw new StockMarketExpection("The stock is not in position. Trader: "
						+ this.name);
			}
			//he cannot sell more stocks than he possesses.
			int orderSizePosessed = getSizeOfStockFromList(symbol, position);
			if (orderSizePosessed < volume) {
				throw new StockMarketExpection("The order volume is more than possessed. Trader: "
						+ this.name + ". Stocks size posessed: " + orderSizePosessed
						+ ", Amount ordered: " + volume);
			}
			order = new SellOrder(symbol, volume, true, this);
			order.setPrice(market.getStockForSymbol(symbol).getPrice());
		}
		ordersPlaced.add(order);
		// Also enter the order into the orderbook of the market.
		market.addOrder(order);


	}

	public void tradePerformed(Order order, double matchPrice)
			throws StockMarketExpection {
		if (order instanceof BuyOrder) {
			double cost = matchPrice * order.getSize();
			if (this.cashInHand >= cost) {
				this.cashInHand = this.cashInHand - cost;
			}
			else {
				throw new StockMarketExpection("Invalid order! Cannot perform order of size "
						+ order.getSize() + " because trader " + this.name + " does not have"
						+ " enough cash to buy " + order.getStockSymbol());
			}
			//removing the order from orders placed
			removeOrderFromList(order.getStockSymbol(), ordersPlaced);
			order.setPrice(matchPrice);
			//adding the order to position
			position.add(order);
		}
		else {
			int orderSize = getSizeOfStockFromList(order.getStockSymbol(), position);
			if (order.getSize() > orderSize) {
				throw new StockMarketExpection("Invalid order! Cannot perform order of size "
						+ order.getSize() + " because trader " + this.name + " does not have"
						+ " enough stock of type " + order.getStockSymbol());
			}
			double revenue = matchPrice * order.getSize();
			this.cashInHand = this.cashInHand + revenue;
			removeOrderFromList(order.getStockSymbol(), position);
			removeOrderFromList(order.getStockSymbol(), ordersPlaced);
			//adding the difference of current stocks and sold ones back to position
			int remainingSize = orderSize - order.getSize();
			if (remainingSize > 0) {
				BuyOrder newOrder = new BuyOrder(order.getStockSymbol(), remainingSize, matchPrice, this);
				position.add(newOrder);
			}
		}
	}

	public void printTrader() {
		System.out.println("Trader Name: " + name);
		System.out.println("=====================");
		System.out.println("Cash: " + cashInHand);
		System.out.println("Stocks Owned: ");
		for (Order order : position) {
			order.printStockNameInOrder();
		}
		System.out.println("Stocks Desired: ");
		for (Order order : ordersPlaced) {
			order.printOrder();
		}
		System.out.println("+++++++++++++++++++++");
		System.out.println("+++++++++++++++++++++");
	}

	public ArrayList<Order> findOrderInList(String symbol, ArrayList<Order> list) {
		ArrayList<Order> order = new ArrayList<Order>();
		for (Order o: list) {
			if (o.getStockSymbol().equals(symbol)) {
				order.add(o);
			}
		}
		return order;
	}

	public int getSizeOfStockFromList(String symbol, ArrayList<Order> list) {
		int size = 0;
		for (Order o : findOrderInList(symbol, list)) {
			size = size + o.getSize();
		}
		return size;
	}

	public void removeOrderFromList(String symbol, ArrayList<Order> list) {
		int n = list.size();
		ArrayList<Integer> valuesToBeRemoved = new ArrayList<Integer>();
		for (int i = 0; i < n; i++) {
			if (list.get(i).getStockSymbol().equals(symbol)) {
				valuesToBeRemoved.add(i);
			}
		}
		int j = 0;
		for (int i : valuesToBeRemoved) {
			Order temp = list.get(i);
			list.set(i, list.get(n - 1 - j));
			list.set(n - 1 - j, temp);
			j++;
		}

		for (int i : valuesToBeRemoved) {
			list.remove(list.size() - 1);
		}
	}
}
