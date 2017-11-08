package net.akehurst.language.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.akehurst.holser.reflect.BetterMethodFinder;
import net.akehurst.language.core.analyser.IGrammarLoader;
import net.akehurst.language.core.analyser.ISemanticAnalyser;
import net.akehurst.language.core.analyser.UnableToAnalyseExeception;
import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParseTreeVisitor;

public abstract class SemanticAnalyserVisitorBasedAbstract implements ISemanticAnalyser, IParseTreeVisitor<Object, Object, UnableToAnalyseExeception> {

	static Class<?>[] parameterTypes = BranchHandler.class.getMethods()[0].getParameterTypes();

	static public interface BranchHandler<T> {
		T handle(IBranch target, List<IBranch> children, Object arg) throws UnableToAnalyseExeception;
	}

	public SemanticAnalyserVisitorBasedAbstract() {
		this.branchHandlers = new HashMap<>();
	}

	private IGrammarLoader grammarLoader;
	private final Map<String, BranchHandler> branchHandlers;

	protected void register(final String branchName, final BranchHandler handler) {
		this.branchHandlers.put(branchName, handler);
	}

	private <T> BranchHandler<T> getBranchHandler(final String branchName) throws UnableToAnalyseExeception {
		BranchHandler<T> handler = this.branchHandlers.get(branchName);
		if (null == handler) {
			try {
				final BetterMethodFinder bmf = new BetterMethodFinder(this.getClass());
				final Method m = bmf.findMethod(branchName, SemanticAnalyserVisitorBasedAbstract.parameterTypes);
				handler = (target, children, arg) -> {
					try {
						return (T) m.invoke(this, target, children, arg);
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						throw new UnableToAnalyseExeception("Error invoking method named " + branchName, e);
					}
				};
				this.register(branchName, handler);
			} catch (final NoSuchMethodException e) {
				throw new UnableToAnalyseExeception("Cannot find analyser method named " + branchName, e);
			}

		}
		return handler;
	}

	protected <T> T analyse(final IBranch branch, final Object arg) throws UnableToAnalyseExeception {
		return null == branch ? null : (T) branch.accept(this, arg);
	}

	// --- ISemanticAnalyser ---
	@Override
	public <T> T analyse(final Class<T> targetType, final IParseTree tree) throws UnableToAnalyseExeception {
		return (T) this.visit(tree, null);
	}

	@Override
	public IGrammarLoader getGrammarLoader() {
		return this.grammarLoader;
	}

	@Override
	public void setGrammarLoader(final IGrammarLoader value) {
		this.grammarLoader = value;
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub

	}

	// --- IParseTreeVisitor ---
	@Override
	public Object visit(final IParseTree target, final Object arg) throws UnableToAnalyseExeception {
		return target.getRoot().accept(this, arg);
	}

	@Override
	public Object visit(final ILeaf target, final Object arg) throws UnableToAnalyseExeception {
		return target.getMatchedText();
	}

	@Override
	public Object visit(final IBranch target, final Object arg) throws UnableToAnalyseExeception {
		final String branchName = target.getName();
		final BranchHandler<?> handler = this.getBranchHandler(branchName);
		if (null == handler) {
			throw new UnableToAnalyseExeception("Branch not handled in analyser " + branchName, null);
		} else {
			final List<IBranch> branchChildren = target.getBranchNonSkipChildren();// .stream().map(it -> it.getIsEmpty() ? null :
																					// it).collect(Collectors.toList());
			return handler.handle(target, branchChildren, arg);
		}

	}

}
