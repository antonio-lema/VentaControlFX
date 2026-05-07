package com.mycompany.ventacontrolfx.application.usecase;

import com.mycompany.ventacontrolfx.domain.model.CartItem;
import com.mycompany.ventacontrolfx.domain.model.Client;
import com.mycompany.ventacontrolfx.domain.model.SuspendedCart;
import com.mycompany.ventacontrolfx.domain.model.SuspendedCartItem;
import com.mycompany.ventacontrolfx.domain.repository.ISuspendedCartRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class SuspendedCartUseCase {

    private final ISuspendedCartRepository repository;

    public SuspendedCartUseCase(ISuspendedCartRepository repository) {
        this.repository = repository;
    }

    public void suspendCart(String alias, List<CartItem> items, Client client, int userId, double total)
            throws SQLException {
        SuspendedCart cart = new SuspendedCart();
        cart.setAlias(alias);
        cart.setUserId(userId);
        if (client != null) {
            cart.setClientId(client.getId());
        }
        cart.setTotal(total);

        List<SuspendedCartItem> suspendedItems = items.stream().map(item -> {
            SuspendedCartItem si = new SuspendedCartItem();
            si.setProduct(item.getProduct());
            si.setQuantity(item.getQuantity());
            si.setPriceAtSuspension(item.getProduct().getPrice());
            return si;
        }).collect(Collectors.toList());

        cart.setItems(suspendedItems);
        repository.save(cart);
    }

    public List<SuspendedCart> listAllSuspended() throws SQLException {
        return repository.findAll();
    }

    public List<SuspendedCart> listUserSuspended(int userId) throws SQLException {
        return repository.findByUserId(userId);
    }

    public SuspendedCart getById(int id) throws SQLException {
        return repository.findById(id);
    }

    public void deleteCart(int id) throws SQLException {
        repository.delete(id);
    }
}

