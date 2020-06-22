package sample.interactor

import sample.repository.MyRepository

class MyInteractor(
    private val myRepository: MyRepository
) {

    fun asd() = myRepository.asd()

    fun zxc() {
        println("ZXC")
    }
}