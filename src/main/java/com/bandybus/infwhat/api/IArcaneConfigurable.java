package com.bandybus.infwhat.api;

/**
 * Marks the Thaumcraft-linked arcane machines that the Ord-Record shard can configure.
 *
 * <p>The marker carries no behaviour of its own. It exists so the shard can accept exactly the
 * arcane machine family without referring to tile entity classes that only load when Thaumcraft
 * is installed.
 */
public interface IArcaneConfigurable extends IConfigurableMachine {
}
