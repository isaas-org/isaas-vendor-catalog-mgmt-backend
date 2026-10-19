package org.isaas.vendorcatalog.identity;

import org.springframework.core.MethodParameter;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/** Lets controllers take a {@link CurrentUser} parameter. */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

	private final CurrentUserService currentUsers;

	public CurrentUserArgumentResolver(CurrentUserService currentUsers) {
		this.currentUsers = currentUsers;
	}

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		return CurrentUser.class.equals(parameter.getParameterType());
	}

	@Override
	public CurrentUser resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
		return currentUsers.resolve(SecurityContextHolder.getContext().getAuthentication());
	}

}
