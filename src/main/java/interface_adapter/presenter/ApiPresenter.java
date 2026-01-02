package interface_adapter.presenter;

import entity.EventResponse;
import usecase.create.CreateEventOutputBoundary;

public class ApiPresenter implements CreateEventOutputBoundary {

    private EventResponse response;

    @Override
    public void presentSuccess(EventResponse response) {
        this.response = response;
    }

    @Override
    public void presentFailure(EventResponse response) {
        this.response = response;
    }

    public EventResponse getResponse() {
        return response;
    }
}