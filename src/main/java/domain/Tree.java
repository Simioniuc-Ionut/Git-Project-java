package domain;

import domain.GitObject;

import domain.tree.TreeEntry;
import java.util.List;
public record Tree(List<TreeEntry> entries) implements GitObject {}