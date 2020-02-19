package rethrower.interactor

import rethrower.Hide
import rethrower.repository.MyRepository

class MyInteractor(
    private val myRepository: MyRepository
) {

    fun asd() = myRepository.asd()

    fun zxc() {
        println("ZXC")
    }
}